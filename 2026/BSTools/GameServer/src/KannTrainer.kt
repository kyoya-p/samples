import bstools.model.*
import kotlin.random.Random

/**
 * KANN(cinterop連携したC言語のNNライブラリ)の価値ネットワークを、完全ランダムなプレイの
 * 自己対戦から学習する。
 *
 * ESベースのTuner.kt/RnnEvaluator.ktとは異なり、こちらは「勝敗そのもの」を教師ラベルとした
 * 本物のBPTT学習(KannValueNetwork.trainStep)を使う。局面を1つずつ記録し、対局終了後に
 * その対局の最終結果(勝1.0/負0.0/引分0.5)を、記録した各局面のプレイヤー視点で遡ってラベル
 * 付けする(モンテカルロ収益のバックプロパゲーション)。
 *
 * 選択は評価値の最大化ではなく完全ランダム(一様分布)で行う。学習前のネットワークで
 * 評価値greedyに選んでも意味のあるバイアスにしかならないため、素朴な探索から始める。
 */

/** 1局中に記録した1局面ぶんの学習前データ (ラベルは対局終了後に確定するため後付け) */
private data class KannExample(val tokens: List<DoubleArray>, val globals: DoubleArray, val playerId: Int)

/**
 * ランダム方策で1局最後まで進め、各局面の記録と勝者を返す。
 * メインステップ等は「列挙された手(forbiddenを除く)」+「ステップ終了」を一様分布から選ぶ。
 * ブロック宣言は列挙された手(ブロック/ブロックしない)自体に「ブロックしない」が
 * 含まれているため、追加の選択肢は不要。
 */
private fun playRandomGame(state: GameState, rng: Random, maxSteps: Int): List<KannExample> {
    val examples = mutableListOf<KannExample>()
    repeat(maxSteps) {
        if (state.winner != null) return examples

        val p = state.choosingPlayer
        val opp = state.notChoosingPlayer
        examples.add(KannExample(buildTokenSequence(p, opp), buildGlobalFeatures(state, p, opp), state.choosingPlayerId))

        val actions = enumerateActions(state)
        val messages = mutableListOf<String>()
        val candidates = actions.filterNot { it.forbidden }

        if (state.step == Step.BLOCK_DECLARATION) {
            val choice = candidates.randomOrNull(rng)
            if (choice != null) applyActionAndRecord(state, choice, messages)
        } else {
            // 列挙された合法手 + 「ステップ終了」を一様分布から選ぶ
            val pick = rng.nextInt(candidates.size + 1)
            if (pick < candidates.size) {
                applyActionAndRecord(state, candidates[pick], messages)
            } else {
                advanceStep(state, messages)
                state.visitedPositions.add(positionHash(state))
            }
        }
    }
    return examples
}

/** 0.0〜1.0 を小数第1位までの % 文字列にする (Kotlin/Native には String.format が無いため手書き) */
private fun pct(v: Double): String {
    val hundredths = (v * 1000).toInt()
    return "${hundredths / 10}.${hundredths % 10}%"
}

/** 小数第4位までの文字列にする (Kotlin/Native には String.format が無いため手書き) */
private fun fmt4(v: Double): String {
    val scaled = (v * 10000).toInt()
    val sign = if (scaled < 0) "-" else ""
    val a = kotlin.math.abs(scaled)
    val intPart = a / 10000
    val fracPart = a % 10000
    val fracStr = fracPart.toString().padStart(4, '0')
    return "$sign$intPart.$fracStr"
}

/**
 * `--kann-train` 起動時のエントリポイント。
 * `--games` `--max-steps` `--lr` `--seed` `--out` で調整できる。
 * `--no-train-debug` を付けると `KannValueNetwork.trainStep` を呼ばず、ランダムプレイに
 * よるデータ収集だけを行う(切り分け用の診断オプション。データ生成側の問題と
 * KANN側の問題を区別するために使う)。
 */
fun runKannRandomTraining(args: Array<String>) {
    fun intArg(flag: String, default: Int): Int {
        val idx = args.indexOf(flag)
        return if (idx >= 0 && idx + 1 < args.size) args[idx + 1].toIntOrNull() ?: default else default
    }
    fun doubleArg(flag: String, default: Double): Double {
        val idx = args.indexOf(flag)
        return if (idx >= 0 && idx + 1 < args.size) args[idx + 1].toDoubleOrNull() ?: default else default
    }

    val noTrainDebug = args.contains("--no-train-debug")
    val games = intArg("--games", 200)
    val maxSteps = intArg("--max-steps", 150)
    val lr = doubleArg("--lr", 0.01).toFloat()
    val seed = intArg("--seed", 1)
    val outIdx = args.indexOf("--out")
    val outPath = if (outIdx >= 0 && outIdx + 1 < args.size) args[outIdx + 1] else "kann-value.bin"

    val rng = Random(seed)
    val net = KannValueNetwork()

    println("=== KANN価値ネットワーク: ランダムプレイからの学習開始 (教師ラベル=勝敗そのもの) ===")
    println("対局数=$games, 手数上限=$maxSteps, 学習率=$lr")
    println()

    var wins1 = 0
    var wins2 = 0
    var draws = 0
    var totalExamples = 0
    var costAccum = 0.0

    for (g in 1..games) {
        val gameSeed = rng.nextLong()
        val state = createInitialGameState(seed = gameSeed)
        val examples = playRandomGame(state, rng, maxSteps)
        val winner = state.winner
        when (winner) {
            1 -> wins1++
            2 -> wins2++
            else -> draws++
        }

        for (ex in examples) {
            val label = when {
                winner == null -> 0.5
                winner == ex.playerId -> 1.0
                else -> 0.0
            }
            val cost = if (noTrainDebug) 0f else net.trainStep(ex.tokens, ex.globals, label, lr)
            costAccum += cost
            totalExamples++
        }

        if (g % 20 == 0 || g == games) {
            val avgCost = if (totalExamples > 0) costAccum / totalExamples else 0.0
            println("対局$g/$games: 累計 勝敗=P1:$wins1/P2:$wins2/引分:$draws, 学習例数=$totalExamples, 平均コスト=${fmt4(avgCost)}")
        }
    }

    println()
    println("=== 学習完了 ===")
    println("勝敗内訳: P1=${pct(wins1.toDouble() / games)} P2=${pct(wins2.toDouble() / games)} 引分=${pct(draws.toDouble() / games)}")
    net.save(outPath)
    println("=== 学習済みモデルを保存: $outPath ===")
    net.delete()
}
