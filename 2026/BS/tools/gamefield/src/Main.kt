package gamefield

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import java.io.File

class SimulateCommand : CliktCommand(name = "gamefield") {
    private val deck1 by option("--deck1", "-d1", help = "デッキ1のファイルパス (YAML/Markdown)").default("deck-fara.yaml")
    private val deck2 by option("--deck2", "-d2", help = "デッキ2のファイルパス (YAML/Markdown)").default("deck-kogyo.yaml")
    private val train by option("--train", "-t", help = "強化学習 (Self-Play) を実行する").flag(default = false)
    private val epochs by option("--epochs", "-e", help = "学習エポック数").int().default(10)

    override fun run() {
        val model = NeuralBoardEvaluator()
        val modelFile = File("temp/gamefield_model.json")
        if (modelFile.exists()) {
            if (model.load(modelFile)) {
                println("学習済みモデルをロードしました: ${modelFile.path}")
            }
        }

        if (train) {
            SelfPlayTrainer.train(model, epochs, deck1, deck2)
            return
        }

        runInteractiveSimulator(deck1, deck2, model)
    }

    private fun runInteractiveSimulator(deck1Path: String, deck2Path: String, model: NeuralBoardEvaluator) {
        println("=================================================================")
        println("⚔ バトルスピリッツ CLI シミュレータ & 強化学習局面評価器 (gamefield)")
        println("=================================================================")
        println("デッキ1: $deck1Path")
        println("デッキ2: $deck2Path\n")

        val state = RuleEngine.setupInitialState(deck1Path, deck2Path)
        val visitedStates = mutableSetOf<String>()

        while (state.currentStep != Step.GAME_OVER) {
            val currentHash = RuleEngine.calculateStateHash(state)
            visitedStates.add(currentHash)

            printState(state)

            val rawActions = RuleEngine.generateLegalActions(state)
            if (rawActions.isEmpty()) {
                println("実行可能なアクションがありません。自動的にステップを進めます。")
                state.currentStep = Step.END
                RuleEngine.applyAction(state, Action.EndStep)
                continue
            }

            // 各アクションにAI評価値を付与
            val actionInfos = rawActions.mapIndexed { index, action ->
                val label = describeAction(state, action)
                val isForbidden = RuleEngine.isForbiddenAction(state, action, visitedStates)
                val eval = if (isForbidden) -2.0f else RuleEngine.evaluateAction(model, state, action)
                val reason = if (isForbidden) "同一盤面ループ" else null
                ActionInfo(
                    action = action,
                    category = label.first,
                    detail = label.second,
                    eval = eval,
                    isForbidden = isForbidden,
                    forbiddenReason = reason
                )
            }.sortedWith(compareByDescending<ActionInfo> { it.eval ?: -999.0f }.thenBy { it.isForbidden })

            println("\n【選択可能なアクション一覧】")
            for ((idx, info) in actionInfos.withIndex()) {
                val evalStr = info.eval?.let { String.format(" [AI評価値: %+.3f]", it) } ?: ""
                val forbiddenPrefix = if (info.isForbidden) "🚫[${info.forbiddenReason}] " else ""
                val isBest = (idx == 0 && !info.isForbidden)
                val bestMark = if (isBest) " ★(推奨)" else ""
                println("  ${idx + 1}: ${forbiddenPrefix}${info.category} - ${info.detail}${evalStr}${bestMark}")
            }

            println("  n: ステップ終了 / パス")
            println("  q: サレンダー (投了)")
            println("  Enter / a: AI自動決定 (評価値最上位のアクションを実行)")

            print("\n選択してください [1-${actionInfos.size}, n:終了, q:サレンダー, Enter:AI]: ")
            val input = readLine()?.trim() ?: ""

            if (input.equals("q", ignoreCase = true)) {
                println("サレンダーしました。ゲームを終了します。")
                break
            }

            val chosenInfo: ActionInfo? = if (input.isEmpty() || input.equals("a", ignoreCase = true)) {
                val best = actionInfos.firstOrNull { !it.isForbidden }
                if (best != null) {
                    println(">> AI自動決定: ${best.category} (${String.format("%+.3f", best.eval)})")
                    best
                } else {
                    println("有効なアクションがありません。")
                    null
                }
            } else if (input.equals("n", ignoreCase = true)) {
                val passOrEnd = actionInfos.find { it.action is Action.EndStep || it.action is Action.Pass }
                passOrEnd ?: run {
                    println("現在ステップ終了は選択できません。")
                    null
                }
            } else {
                val num = input.toIntOrNull()
                if (num != null && num in 1..actionInfos.size) {
                    actionInfos[num - 1]
                } else {
                    println("無効な入力です。")
                    null
                }
            }

            if (chosenInfo != null) {
                if (chosenInfo.isForbidden) {
                    println("🚫 このアクションは選択できません (${chosenInfo.forbiddenReason})")
                    continue
                }
                RuleEngine.applyAction(state, chosenInfo.action)
                RuleEngine.processAutomaticSteps(state)
            }
        }

        println("\n=======================================================")
        val winner = if (state.player.life <= 0) "プレイヤー2 (相手)" else "プレイヤー1 (自分)"
        println("🏁 ゲーム終了！ 勝者: $winner")
        println("=======================================================")
    }

    private fun describeAction(state: GameState, action: Action): Pair<String, String> {
        return when (action) {
            is Action.PlayCard -> {
                val payStr = if (action.paySoul) "${action.payNormal + 1}s" else "${action.payNormal}"
                val placeStr = if (action.placeSoul) "${action.placeNormal + 1}s" else "${action.placeNormal}"
                "【召喚/配置】 ${action.cardName}" to "支払コスト: $payStr, 配置コア: $placeStr"
            }
            is Action.MoveCore -> {
                val countStr = if (action.soul) "1s" else "${action.normal}"
                "【コア移動】 ${action.fromId} -> ${action.toId}" to "移動コア: $countStr"
            }
            is Action.Attack -> {
                "【アタック宣言】 ${action.attackerName}" to "疲労してアタック"
            }
            is Action.Block -> {
                "【ブロック宣言】 ${action.blockerName}" to "疲労してブロック"
            }
            is Action.Pass -> {
                "【パス】" to "フラッシュまたはブロックをパス"
            }
            is Action.EndStep -> {
                "【ステップ終了】" to "現在のステップを終了し次へ進む"
            }
            is Action.Surrender -> {
                "【サレンダー】" to "ゲームを降伏終了"
            }
        }
    }

    private fun printState(state: GameState) {
        val active = state.activeSide
        val defending = state.defendingSide
        val isP1 = (state.activePlayerId == 1)
        val p1Name = if (isP1) "自分 (P1)" else "相手 (P1)"
        val p2Name = if (isP1) "相手 (P2)" else "自分 (P2)"

        println("\n-------------------------------------------------------")
        println("【ターン ${state.turnCount}】 手番: プレイヤー${state.activePlayerId} / ステップ: ${state.currentStep.displayName}")
        println("-------------------------------------------------------")
        println("相手 ライフ: [${defending.life}/5], リザーブ: ${defending.reserve.format()}, トラッシュコア: ${defending.trashCores.format()}, 手札: ${defending.hand.size}枚, 山札: ${defending.deck.size}枚")
        print("相手 フィールド: ")
        if (defending.field.isEmpty()) println("(なし)")
        else println(defending.field.joinToString(", ") { "${it.name}(${it.cores.format()})${if (it.isExhausted) "[疲労]" else ""}" })

        println()
        println("自分 ライフ: [${active.life}/5], リザーブ: ${active.reserve.format()}, トラッシュコア: ${active.trashCores.format()}, 山札: ${active.deck.size}枚")
        print("自分 フィールド: ")
        if (active.field.isEmpty()) println("(なし)")
        else println(active.field.joinToString(", ") { "${it.name}(${it.cores.format()})${if (it.isExhausted) "[疲労]" else ""}" })

        println("自分 手札:")
        if (active.hand.isEmpty()) println("  (なし)")
        else {
            for (c in active.hand) {
                val red = RuleEngine.calculateReduction(c, active.field)
                val eff = maxOf(0, c.cost - red)
                println("  - ${c.name} (コスト:${c.cost}, 軽減後:$eff, 系統:${c.systems.joinToString("/")})")
            }
        }
        println("-------------------------------------------------------")
    }
}

fun main(args: Array<String>) = SimulateCommand().main(args)
