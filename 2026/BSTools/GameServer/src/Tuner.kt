import bstools.model.*
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

/**
 * 自己対戦によって AI の評価値の重み (EvalWeights) を自動調整するチューナー。
 *
 * 目的関数は「ゲームに勝つこと」そのもの（勝率）とする。
 * (1+1) 型の山登り法（Evolution Strategy の最も単純な形）で局所探索する:
 *   1. 現在のチャンピオン重みを1パラメータだけ小さく変異させた候補を作る
 *   2. 候補 vs チャンピオンで複数戦の自己対戦を行い、候補側の勝率を測る
 *   3. 勝率が5割を超えれば候補を新チャンピオンとして採用する（超えなければ棄却）
 * これを指定世代数だけ繰り返す。先手/後手の有利を打ち消すため、対戦ごとに
 * どちらの重みをプレイヤー1として使うかを交互に入れ替える。
 */

/**
 * 局面を最後まで自己対戦させ、勝者(1 or 2)を返す。
 * 手数上限に達しても決着しない場合は null (引き分け扱い)。
 *
 * 各プレイヤーは「評価値が最大の選択肢」を選び、それがステップ終了の基準値
 * (`weights.stepEndEval`) を上回らなければステップを進める — Playmats の
 * WebUI が採用しているのと同じ意思決定則を、ブラウザなしで直接シミュレートする。
 */
fun playoutGame(
    weightsByPlayer: Map<Int, EvalWeights>,
    seed: Long,
    format: GameFormat = GameFormat.STANDARD,
    maxSteps: Int = 300
): Int? {
    val state = createInitialGameState(seed = seed, format = format)

    repeat(maxSteps) {
        if (state.winner != null) return state.winner

        val weights = weightsByPlayer[state.choosingPlayerId] ?: EvalWeights()
        activeWeights = weights
        val actions = enumerateActions(state)
        val messages = mutableListOf<String>()

        if (state.step == Step.BLOCK_DECLARATION) {
            // ブロック宣言はステップ終了という選択肢がないので、必ず列挙された手(BLOCK/PASS_BLOCK)から選ぶ
            val choice = actions.filterNot { it.forbidden }.maxByOrNull { it.eval }
            if (choice != null) applyActionAndRecord(state, choice, messages)
        } else {
            val best = actions.filterNot { it.forbidden }.maxByOrNull { it.eval }
            // stepEndEval(state) は activeWeights を見るので、直前に設定した weights がそのまま使われる
            if (best != null && best.eval > stepEndEval(state)) {
                applyActionAndRecord(state, best, messages)
            } else {
                advanceStep(state, messages)
                state.visitedPositions.add(positionHash(state))
            }
        }
    }
    return state.winner
}

/**
 * candidate 側の勝率を baseline との自己対戦で測る。先攻/後攻を交互に入れ替えて先手有利を打ち消す。
 *
 * `games` 局は互いに独立なシミュレーションなので、複数スレッドで並列実行する。
 * 乱数シードは並列実行に入る前に `rng` から順番どおり採番することで、実行順やスレッド数に
 * 依存しない再現性を保つ(`activeWeights` は `@ThreadLocal` 化済みなのでスレッド間で競合しない)。
 */
fun evaluateCandidate(candidate: EvalWeights, baseline: EvalWeights, games: Int, rng: Random, maxSteps: Int = 300): Double = runBlocking(Dispatchers.Default) {
    val seeds = (0 until games).map { rng.nextLong() }
    val results = seeds.mapIndexed { i, seed ->
        async {
            val candidateIsP1 = i % 2 == 0
            val weightsByPlayer = if (candidateIsP1) {
                mapOf(1 to candidate, 2 to baseline)
            } else {
                mapOf(1 to baseline, 2 to candidate)
            }
            val winner = playoutGame(weightsByPlayer, seed, maxSteps = maxSteps)
            when {
                winner == null -> 0.5 // 手数上限に達した引き分けは五分とみなす
                candidateIsP1 && winner == 1 -> 1.0
                !candidateIsP1 && winner == 2 -> 1.0
                else -> 0.0
            }
        }
    }.awaitAll()
    results.sum() / games
}

/** 0.0〜1.0 を小数第1位までの % 文字列にする (Kotlin/Native には String.format が無いため手書き) */
private fun pct(v: Double): String {
    val hundredths = (v * 1000).toInt()
    return "${hundredths / 10}.${hundredths % 10}%"
}

/**
 * 山登り法で `generations` 世代ぶん重みを改良し、最終的な重みを返す。
 * 各世代ごとに採用/棄却と勝率をログ出力するので、途中経過が追える。
 */
fun tuneWeights(generations: Int, gamesPerGeneration: Int, sigma: Double, rng: Random, maxSteps: Int = 300): EvalWeights {
    var champion = EvalWeights()
    var adoptedCount = 0

    println("=== AI評価値の自動チューニング開始 (目的関数: 自己対戦の勝率) ===")
    println("世代数=$generations, 1世代あたりの対戦数=$gamesPerGeneration, 摂動幅=$sigma, 手数上限=$maxSteps")
    println()

    for (gen in 1..generations) {
        val candidate = champion.mutate(rng, sigma)
        val winRate = evaluateCandidate(candidate, champion, gamesPerGeneration, rng, maxSteps)
        val adopted = winRate > 0.5

        val changedField = EvalWeights.names.firstOrNull { champion.get(it) != candidate.get(it) }
        val fieldNote = if (changedField != null) {
            "$changedField: ${champion.get(changedField)} → ${candidate.get(changedField)}"
        } else "変化なし"

        println("世代$gen: 候補勝率=${pct(winRate)} [$fieldNote] ${if (adopted) "→ 採用" else "→ 棄却"}")

        if (adopted) {
            champion = candidate
            adoptedCount++
        }
    }

    println()
    println("=== チューニング完了 ($adoptedCount / $generations 世代で採用) ===")
    println(champion.toKotlinLiteral())
    return champion
}

// ===================== RNN版 (evaluateStateWithRnn を使う自己対戦) =====================

/** RNNモードで1局シミュレートする。各手番ごとに `rnnParams` をその手番のプレイヤーの重みに差し替える */
fun playoutGameRnn(
    paramsByPlayer: Map<Int, RnnParams>,
    seed: Long,
    format: GameFormat = GameFormat.STANDARD,
    maxSteps: Int = 300
): Int? {
    val state = createInitialGameState(seed = seed, format = format)
    evalMode = EvalMode.RNN

    repeat(maxSteps) {
        if (state.winner != null) return state.winner

        rnnParams = paramsByPlayer[state.choosingPlayerId]
        val actions = enumerateActions(state)
        val messages = mutableListOf<String>()

        if (state.step == Step.BLOCK_DECLARATION) {
            val choice = actions.filterNot { it.forbidden }.maxByOrNull { it.eval }
            if (choice != null) applyActionAndRecord(state, choice, messages)
        } else {
            val best = actions.filterNot { it.forbidden }.maxByOrNull { it.eval }
            // stepEndEval(state) は rnnParams(直前に設定済み) を見て、選択肢と同じ評価器で計算する
            if (best != null && best.eval > stepEndEval(state)) {
                applyActionAndRecord(state, best, messages)
            } else {
                advanceStep(state, messages)
                state.visitedPositions.add(positionHash(state))
            }
        }
    }
    return state.winner
}

/**
 * candidate 側の勝率を baseline との自己対戦で測る (RNN版)。
 * RNNはGRUの順伝播をアクション候補ごとに毎回行うため線形モデルより1局あたりの計算コストが重く、
 * 並列化による恩恵が特に大きい。設計は [evaluateCandidate] と同じ
 * (乱数シードを並列実行前に順番どおり採番して再現性を保つ)。
 */
fun evaluateCandidateRnn(candidate: RnnParams, baseline: RnnParams, games: Int, rng: Random, maxSteps: Int = 300): Double = runBlocking(Dispatchers.Default) {
    val seeds = (0 until games).map { rng.nextLong() }
    val results = seeds.mapIndexed { i, seed ->
        async {
            val candidateIsP1 = i % 2 == 0
            val paramsByPlayer = if (candidateIsP1) {
                mapOf(1 to candidate, 2 to baseline)
            } else {
                mapOf(1 to baseline, 2 to candidate)
            }
            val winner = playoutGameRnn(paramsByPlayer, seed, maxSteps = maxSteps)
            when {
                winner == null -> 0.5
                candidateIsP1 && winner == 1 -> 1.0
                !candidateIsP1 && winner == 2 -> 1.0
                else -> 0.0
            }
        }
    }.awaitAll()
    results.sum() / games
}

/**
 * RNNの重み(約{RnnParams.paramCount}個)を自己対戦で山登り法(ES)により改良する。
 * EvalWeights と違い1パラメータずつではなく、全パラメータに同時に小さいノイズを加える
 * ([RnnParams.mutate] 参照) — 次元数が大きいため、1つずつ動かす方式では実質的に進まない。
 */
fun tuneRnnWeights(generations: Int, gamesPerGeneration: Int, sigma: Double, rng: Random, maxSteps: Int = 300): RnnParams {
    var champion = RnnParams.random(rng)
    var adoptedCount = 0

    println("=== RNN局面評価器の自動チューニング開始 (目的関数: 自己対戦の勝率) ===")
    println("パラメータ数=${RnnParams.paramCount}, 世代数=$generations, 1世代あたりの対戦数=$gamesPerGeneration, 摂動幅=$sigma, 手数上限=$maxSteps")
    println()

    for (gen in 1..generations) {
        val candidate = champion.mutate(rng, sigma)
        val winRate = evaluateCandidateRnn(candidate, champion, gamesPerGeneration, rng, maxSteps)
        val adopted = winRate > 0.5
        println("世代$gen: 候補勝率=${pct(winRate)} ${if (adopted) "→ 採用" else "→ 棄却"}")
        if (adopted) {
            champion = candidate
            adoptedCount++
        }
    }

    println()
    println("=== チューニング完了 ($adoptedCount / $generations 世代で採用) ===")
    return champion
}

/**
 * `--tune` 起動時のエントリポイント。
 * `--generations` `--games` `--sigma` `--seed` で調整できる。`--rnn` を付けると
 * 従来の EvalWeights(線形モデル)ではなく RnnParams(GRUベースの局面評価器)を学習する。
 */
fun runWeightTuning(args: Array<String>) {
    fun intArg(flag: String, default: Int): Int {
        val idx = args.indexOf(flag)
        return if (idx >= 0 && idx + 1 < args.size) args[idx + 1].toIntOrNull() ?: default else default
    }
    fun doubleArg(flag: String, default: Double): Double {
        val idx = args.indexOf(flag)
        return if (idx >= 0 && idx + 1 < args.size) args[idx + 1].toDoubleOrNull() ?: default else default
    }

    val generations = intArg("--generations", 20)
    val games = intArg("--games", 20)
    val seed = intArg("--seed", 1)
    // チューニングは勝敗の相対比較が目的で、対局の完全な現実味は不要。
    // 本セッションで配置コア枚数の候補を網羅するよう修正した結果、ターンが進むほど
    // 選択肢が数百件規模に膨らむ対局があり、既定の300手上限では山登り法の1世代すら
    // 現実的な時間で終わらないことが実際に起きた。既定を150に下げて実用的な速度にする
    // (本番の Playmats/GameServer 側の maxSteps には影響しない、Tuner専用の値)。
    val maxSteps = intArg("--max-steps", 150)

    if (args.contains("--rnn")) {
        val sigma = doubleArg("--sigma", 0.05)
        val outIdx = args.indexOf("--out")
        val outPath = if (outIdx >= 0 && outIdx + 1 < args.size) args[outIdx + 1] else RNN_WEIGHTS_DEFAULT_PATH
        val champion = tuneRnnWeights(generations, games, sigma, Random(seed), maxSteps)
        if (saveRnnParams(champion, outPath)) {
            println("=== 学習済み重みを保存: $outPath (パラメータ数=${RnnParams.paramCount}) ===")
        } else {
            println("=== 警告: 重みの保存に失敗 ($outPath) ===")
        }
    } else {
        val sigma = doubleArg("--sigma", 0.06)
        tuneWeights(generations, games, sigma, Random(seed), maxSteps)
    }
}

/** 診断用: RNNモードで1局だけ進め、決着したか・何ターンで打ち切られたかを出力する */
fun debugRnnGame(seed: Long, maxSteps: Int = 300) {
    val rng = Random(seed)
    val paramsByPlayer = mapOf(1 to RnnParams.random(rng), 2 to RnnParams.random(Random(rng.nextLong())))
    val state = createInitialGameState(seed = seed)
    evalMode = EvalMode.RNN
    var steps = 0
    repeat(maxSteps) {
        if (state.winner != null) return@repeat
        steps++
        rnnParams = paramsByPlayer[state.choosingPlayerId]
        val actions = enumerateActions(state)
        val messages = mutableListOf<String>()
        if (state.step == Step.BLOCK_DECLARATION) {
            val choice = actions.filterNot { it.forbidden }.maxByOrNull { it.eval }
            if (choice != null) applyActionAndRecord(state, choice, messages)
        } else {
            val best = actions.filterNot { it.forbidden }.maxByOrNull { it.eval }
            val endEval = stepEndEval(state)
            if (steps <= 20 || steps % 20 == 0) {
                println("step=$steps turn=${state.turn} phase=${state.step} best=${best?.type}:${best?.eval} endEval=$endEval field=${state.player1.field.size}/${state.player2.field.size}")
            }
            if (best != null && best.eval > endEval) {
                applyActionAndRecord(state, best, messages)
            } else {
                advanceStep(state, messages)
                state.visitedPositions.add(positionHash(state))
            }
        }
    }
    println("=== 結果: winner=${state.winner}, turn=${state.turn}, steps=$steps (maxSteps=$maxSteps) ===")
}
