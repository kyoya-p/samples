import bstools.model.*
import kotlin.random.Random

/**
 * KANN(cinterop連携したC言語のNNライブラリ)の価値ネットワークを、自己対戦を通じて
 * 序盤の選択を含めすべての状態(意思決定前・意思決定直後)を入力・記録し、
 * 最終結果(勝敗: 勝=1.0 / 負=0.0 / 手数切れ引分=0.0)に応じて序盤の局面評価値まで
 * BPTT(誤差逆伝播)で直接誤差修正する学習ループ。
 */

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
 * `--games` `--max-steps` `--lr` `--epsilon` `--seed` `--resume` `--out` で調整できる。
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
    fun strArg(flag: String): String? {
        val idx = args.indexOf(flag)
        return if (idx >= 0 && idx + 1 < args.size) args[idx + 1] else null
    }

    val noTrainDebug = args.contains("--no-train-debug")
    val games = intArg("--games", 100)
    val maxSteps = intArg("--max-steps", 300)
    val lr = doubleArg("--lr", 0.0005).toFloat()
    val epsilon = doubleArg("--epsilon", 0.15)
    val seed = intArg("--seed", 1)
    val resumePath = strArg("--resume")
    val outPath = strArg("--out") ?: "weights/bptt_trained.kann.weight"
    val deck1 = strArg("--deck1") ?: strArg("--deck") ?: "deck-kogyo.yaml"
    val deck2 = strArg("--deck2") ?: strArg("--deck") ?: "deck-kogyo.yaml"

    val rng = Random(seed)
    val net = if (resumePath != null) {
        KannValueNetwork.load(resumePath) ?: error("重みファイルを読み込めません: $resumePath")
    } else {
        KannValueNetwork()
    }

    println("=== KANN価値ネットワーク: 序盤〜終盤の全局面BPTT誤差修正学習開始 (教師=最終勝敗) ===")
    println("対局数=$games, 手数上限=$maxSteps, 学習率=$lr, 探索率=$epsilon")
    if (resumePath != null) println("初期重み: $resumePath")
    println()

    var wins1 = 0
    var wins2 = 0
    var draws = 0
    var totalStates = 0
    var costAccum = 0.0

    val nets = mapOf(1 to net, 2 to net)

    for (g in 1..games) {
        val gameSeed = rng.nextLong()
        val res = playoutGameKann(
            netsByPlayer = nets,
            seed = gameSeed,
            maxSteps = maxSteps,
            deck1Name = deck1,
            deck2Name = deck2,
            recordTrajectory = true,
            exploreEpsilon = epsilon,
            rng = rng
        )
        val winner = res.winner
        when (winner) {
            1 -> wins1++
            2 -> wins2++
            else -> draws++
        }

        if (!noTrainDebug && res.trajectory.isNotEmpty()) {
            val avgCost = trainTrajectoryWithOutcome(
                net = net,
                trajectory = res.trajectory,
                winner = winner,
                lr = lr,
                rng = rng,
                targetPlayerId = null
            )
            costAccum += avgCost * res.trajectory.size
            totalStates += res.trajectory.size
        }

        if (g % 10 == 0 || g == games) {
            val avgCost = if (totalStates > 0) costAccum / totalStates else 0.0
            println("対局$g/$games: 累計 勝敗=P1:$wins1/P2:$wins2/引分:$draws, 誤差修正状態数=$totalStates, 平均誤差=${fmt4(avgCost)}")
        }
    }

    println()
    println("=== 学習完了 ===")
    println("勝敗内訳: P1=${pct(wins1.toDouble() / games)} P2=${pct(wins2.toDouble() / games)} 引分=${pct(draws.toDouble() / games)}")
    net.save(outPath)
    println("=== 学習済みモデルを保存: $outPath ===")
    net.delete()
}

