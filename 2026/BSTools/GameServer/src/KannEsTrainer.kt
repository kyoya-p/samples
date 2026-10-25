import bstools.model.*
import bstools.kann.kad_srand
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import platform.posix.*
import kotlin.random.Random

/**
 * KANN(cinterop連携したC言語のNNライブラリ)のGRU価値ネットワークを、
 * 序盤〜終盤の全局面BPTT誤差修正 + 進化戦略(ES)による自己対戦で強化学習する。
 *
 * 並列化設計:
 * `KannValueNetwork.cloneNet()` (`kann_clone`) により対局スロットごとに完全に独立した
 * C側計算グラフ(`kann_t*`)および永続バッファを確保し、`Dispatchers.Default` 上の
 * `async`/`awaitAll` で全対局をマルチスレッド並列実行する。
 */

// 1手先読み評価(evaluateActionWithKann)とステップ終了の基準値(kannStepEndEval)は
// model/src/KannRnnEvaluator.kt の共通実装を使う ([EvalMode.KANN] 経路と重複させない)。

/**
 * 「本当に進行しているか」を外部(GUIの`/api/training-status`)から見えるようにするための
 * 診断用カウンタ。世代・対局番号は[runKannEsTraining]/[evaluateCandidateKann]側で更新し、
 * 手数は[playoutGameKann]の1手ごとに増分する。単調増加が止まっていれば、対局が本当に
 * ハングしている(無限ループ)ことを疑える(逆に増え続けていれば「低速だが生きている」)。
 */
private object EsDiag {
    var generation = 0
    var totalGenerations = 0
    var games = 0
    var adoptedCount = 0
    var outPath = ""
    var gameIndex = 0
    var stepInGame = 0L
    var totalSteps = 0L
    /**
     * 1手ごとに何個の局面(候補行動+ステップ終了の評価値)をKANNへ評価させたかの累計。
     * `totalSteps`(手数)は「何手進んだか」しか表さないが、実際の計算コストは
     * 「1手あたり何個の候補を評価したか」に比例する。手数の増加が鈍っても評価局面数が
     * 順調に増えていれば「1手あたりの候補が多いだけで生きている」、逆に評価局面数まで
     * 止まっていれば本当のハングだと判断できる。
     */
    var evaluatedPositions = 0L
    /**
     * プロセス起動から通算で遭遇した**重複を除いた**局面ハッシュの集合。対局ごとにリセットする
     * [GameState.visitedPositions](同一局面への手を弾くためだけの、対局内限定の集合)とは別物。
     * こちらはプロセス全体を通じて保持し続け、サイズ(size)だけを指標として公開する。
     * 手数(totalSteps)や評価局面数(evaluatedPositions)が増えていても、実は同じ局面を
     * 行ったり来たりしているだけ(相殺されて盤面が実質進んでいない)なら、この値は
     * ほとんど増えない — 逆にこれが順調に増え続けていれば、本当に新しい局面を
     * 探索し続けていることの一番強い証拠になる。
     */
    val distinctPositionsSeen = HashSet<Long>()
}

private fun writeEsHeartbeat(winRate: Double? = null, adopted: Boolean? = null) {
    writeEsTrainingProgress(
        EsTrainingProgress(
            active = true,
            generation = EsDiag.generation,
            totalGenerations = EsDiag.totalGenerations,
            games = EsDiag.games,
            winRate = winRate,
            adopted = adopted,
            adoptedCount = EsDiag.adoptedCount,
            outPath = EsDiag.outPath,
            updatedAtMs = currentTimeMs(),
            gameIndex = EsDiag.gameIndex,
            stepInGame = EsDiag.stepInGame,
            totalSteps = EsDiag.totalSteps,
            evaluatedPositions = EsDiag.evaluatedPositions,
            distinctPositionsSeen = EsDiag.distinctPositionsSeen.size.toLong()
        )
    )
}

/**
 * 対局中の1状態(序盤の選択〜終盤の決着まで)の記録。
 * 意思決定前の状態、および行動適用直後の状態(行動したプレイヤー視点)の両方を記録し、
 * 最終結果(勝敗)確定後に [KannValueNetwork.trainStep] (BPTT誤差逆伝播) で序盤を含む全局面を一括誤差修正する。
 */
data class KannStateRecord(
    val tokens: List<DoubleArray>,
    val globals: DoubleArray,
    val playerId: Int,
    val turn: Int,
    val stepIndex: Int
)

/**
 * 自己対戦1局の結果と、序盤から終盤までの全状態の記録。
 */
data class KannPlayoutResult(
    val winner: Int?,
    val turn: Int,
    val steps: Int,
    val trajectory: List<KannStateRecord> = emptyList(),
    val evaluatedCount: Long = 0L,
    val visitedHashes: Set<Long> = emptySet()
)

/**
 * 1局の序盤から終盤までのすべての記録状態 [trajectory] を入力し、
 * 最終結果 [winner] (勝=1.0, 負=0.0, 引き分け未決着=0.0) を教師ラベルとして
 * [KannValueNetwork.trainStep] (BPTT + RMSprop) により序盤の局面評価値も含め誤差修正を行う。
 *
 * 時系列順のまま連続更新すると直近の手番プレイヤーの勝敗ラベル(1.0または0.0)へ出力が偏るため、
 * [rng] で全状態をシャッフルし、勝者視点(正例)と敗者視点(負例)を均等に混ぜて誤差修正する。
 *
 * @return 平均コスト(誤差)
 */
fun trainTrajectoryWithOutcome(
    net: KannValueNetwork,
    trajectory: List<KannStateRecord>,
    winner: Int?,
    lr: Float,
    rng: Random? = null,
    targetPlayerId: Int? = null
): Double {
    if (trajectory.isEmpty() || lr <= 0f) return 0.0
    val filtered = if (targetPlayerId != null) {
        trajectory.filter { it.playerId == targetPlayerId }
    } else {
        trajectory
    }
    if (filtered.isEmpty()) return 0.0
    val samples = if (rng != null) filtered.shuffled(rng) else filtered
    var costSum = 0.0
    for (rec in samples) {
        // 最終結果(勝敗): 勝者=1.0 (tanh +1.0), 敗者または手数切れ引き分け=0.0 (tanh -1.0)
        val label = when {
            winner == null -> 0.0
            winner == rec.playerId -> 1.0
            else -> 0.0
        }
        costSum += net.trainStep(rec.tokens, rec.globals, label, lr)
    }
    return costSum / samples.size
}

/**
 * 対戦中の各手の選択肢・評価値・採択結果を記録するファイルロガー。
 */
@OptIn(ExperimentalForeignApi::class)
class MatchLogger(private val filePath: String) {
    private var file: CPointer<FILE>? = null

    fun open(): Boolean {
        file = fopen(filePath, "w")
        return file != null
    }

    fun log(text: String = "") {
        val f = file ?: return
        fputs(text + "\n", f)
        fflush(f)
    }

    fun close() {
        file?.let {
            fflush(it)
            fclose(it)
        }
        file = null
    }
}

private fun formatPlayerSummary(p: PlayerState): String {
    val fieldCards = if (p.field.isEmpty()) "空" else p.field.joinToString(", ") { fc ->
        "${fc.name}(Lv${fc.level},コア${fc.cores.format()})"
    }
    return "Life:${p.life}, Reserve:${p.reserve.format()}, Hand:${p.hand.size}枚, Field:[$fieldCards]"
}

private fun fmtScore(v: Double): String {
    val scaled = (v * 10000).toInt()
    val sign = if (scaled < 0) "-" else if (scaled > 0) "+" else " "
    val a = kotlin.math.abs(scaled)
    return "$sign${a / 10000}.${(a % 10000).toString().padStart(4, '0')}"
}

/**
 * 手番ごとにnetの重みを差し替えながら1局最後まで自己対戦させ、勝者(1 or 2)と決着ターン数・手数、
 * および序盤から終盤までの全状態記録([KannStateRecord]のリスト)を返す。
 * 手数上限に達しても決着しない場合は winner = null (引き分け＝両者敗北扱い)。
 * [logger]が指定されている場合、全手の選択肢・評価値・採択結果をログファイルに書き出す。
 */
fun playoutGameKann(
    netsByPlayer: Map<Int, KannValueNetwork>,
    weightsByPlayer: Map<Int, FloatArray> = emptyMap(),
    seed: Long,
    maxSteps: Int,
    logger: MatchLogger? = null,
    playerNames: Map<Int, String> = emptyMap(),
    deck1Name: String = "deck-kogyo.yaml",
    deck2Name: String = "deck-kogyo.yaml",
    format: GameFormat = GameFormat.STANDARD,
    recordTrajectory: Boolean = true,
    exploreEpsilon: Double = 0.0,
    explorePlayers: Set<Int> = emptySet(),
    rng: Random? = null,
    parallelSafe: Boolean = false
): KannPlayoutResult {
    val state = createInitialGameState(deck1Name = deck1Name, deck2Name = deck2Name, seed = seed, format = format)
    val trajectory = if (recordTrajectory) ArrayList<KannStateRecord>(maxSteps * 2) else emptyList<KannStateRecord>()
    var localEvaluated = 0L
    val localVisited = if (parallelSafe) HashSet<Long>() else null

    repeat(maxSteps) { step ->
        if (state.winner != null) {
            if (logger != null) {
                val winnerName = playerNames[state.winner] ?: "Player ${state.winner}"
                logger.log("  【決着】 勝者: $winnerName (${state.turn}ターン / $step 手)")
            }
            return KannPlayoutResult(
                winner = state.winner,
                turn = state.turn,
                steps = step,
                trajectory = trajectory,
                evaluatedCount = localEvaluated,
                visitedHashes = localVisited ?: emptySet()
            )
        }

        val h = positionHash(state)
        if (parallelSafe) {
            localVisited?.add(h)
        } else {
            EsDiag.stepInGame = step.toLong()
            EsDiag.totalSteps++
            EsDiag.distinctPositionsSeen.add(h)
            if (step % 5 == 0) writeEsHeartbeat()
        }

        val choosingId = state.choosingPlayerId
        val choosingName = playerNames[choosingId] ?: "Player $choosingId"
        val currentTurn = state.turn

        // ① 意思決定(選択)前の状態を記録 (序盤のTurn 1からすべて記録)
        if (recordTrajectory && trajectory is MutableList) {
            val pBefore = if (choosingId == 1) state.player1 else state.player2
            val oppBefore = if (choosingId == 1) state.player2 else state.player1
            trajectory.add(
                KannStateRecord(
                    tokens = buildTokenSequence(pBefore, oppBefore),
                    globals = buildGlobalFeatures(state, pBefore, oppBefore),
                    playerId = choosingId,
                    turn = currentTurn,
                    stepIndex = step
                )
            )
        }

        if (logger != null) {
            logger.log("Turn ${state.turn} | 手数 $step | 手番: $choosingName | ${state.step.displayName}")
            logger.log("  盤面:")
            logger.log("    P1: ${formatPlayerSummary(state.player1)}")
            logger.log("    P2: ${formatPlayerSummary(state.player2)}")
        }

        val net = netsByPlayer[choosingId] ?: return@repeat
        weightsByPlayer[choosingId]?.let { net.setWeights(it) }
        val actions = enumerateActions(state)
        val messages = mutableListOf<String>()
        val candidates = actions.filterNot { it.forbidden || it.repeatsPosition }
        val scored = candidates.map { it to evaluateActionWithKann(state, it, net) }
        localEvaluated += candidates.size
        if (!parallelSafe) {
            EsDiag.evaluatedPositions += candidates.size
        }
        val best = scored.maxByOrNull { it.second }

        if (logger != null) {
            logger.log("  候補行動 (${candidates.size}件):")
            for ((idx, pair) in scored.withIndex()) {
                val (act, sc) = pair
                logger.log("    [$idx] ${act.type} | ${act.detail} -> eval: ${fmtScore(sc)}")
            }
        }

        val doExplore = exploreEpsilon > 0.0 && rng != null &&
            (explorePlayers.isEmpty() || choosingId in explorePlayers) &&
            rng.nextDouble() < exploreEpsilon

        if (state.step == Step.BLOCK_DECLARATION) {
            val chosen = if (doExplore && candidates.isNotEmpty()) {
                val pick = candidates.random(rng)
                scored.find { it.first == pick } ?: best
            } else {
                best
            }
            if (chosen != null) {
                if (logger != null) {
                    logger.log("  => 選択: ${chosen.first.type} | ${chosen.first.detail} (eval: ${fmtScore(chosen.second)})")
                }
                applyActionAndRecord(state, chosen.first, messages)
            } else {
                if (logger != null) {
                    logger.log("  => 選択: ブロック可能な候補なし (スルー)")
                }
            }
        } else {
            val endEval = kannStepEndEval(state, net)
            localEvaluated++
            if (!parallelSafe) {
                EsDiag.evaluatedPositions++
            }
            if (logger != null) {
                logger.log("  ステップ終了評価: ${fmtScore(endEval)}")
            }
            if (doExplore && candidates.isNotEmpty()) {
                // 探索時: 評価値上位3件の合法手 + ステップ終了 の中からランダムに選択し、
                // 無意味な自滅手(スピリットLv0消滅)を避けつつ多様な序盤・中盤の有力選択を経験させる
                val topCandidates = scored.sortedByDescending { it.second }.take(3).map { it.first }
                val pickIdx = rng.nextInt(topCandidates.size + 1)
                if (pickIdx < topCandidates.size) {
                    val chosenAct = topCandidates[pickIdx]
                    applyActionAndRecord(state, chosenAct, messages)
                } else {
                    advanceStep(state, messages)
                    state.visitedPositions.add(positionHash(state))
                }
            } else if (best != null && best.second > endEval) {
                if (logger != null) {
                    logger.log("  => 選択: ${best.first.type} | ${best.first.detail} (eval: ${fmtScore(best.second)} > 終了評価 ${fmtScore(endEval)})")
                }
                applyActionAndRecord(state, best.first, messages)
            } else {
                if (logger != null) {
                    val reason = if (best != null) " (候補最大 ${fmtScore(best.second)} <= 終了評価 ${fmtScore(endEval)})" else ""
                    logger.log("  => 選択: ステップ終了$reason")
                }
                advanceStep(state, messages)
                state.visitedPositions.add(positionHash(state))
            }
        }

        // ② 意思決定(選択)適用直後の状態を、選択したプレイヤー(choosingId)視点で記録
        // (evaluateActionWithKann / kannStepEndEval が参照する状態そのものを最終勝敗で誤差修正するため)
        if (recordTrajectory && trajectory is MutableList) {
            val pAfter = if (choosingId == 1) state.player1 else state.player2
            val oppAfter = if (choosingId == 1) state.player2 else state.player1
            trajectory.add(
                KannStateRecord(
                    tokens = buildTokenSequence(pAfter, oppAfter),
                    globals = buildGlobalFeatures(state, pAfter, oppAfter),
                    playerId = choosingId,
                    turn = currentTurn,
                    stepIndex = step
                )
            )
        }

        if (logger != null) {
            if (messages.isNotEmpty()) {
                for (msg in messages) {
                    logger.log("     効果/結果: $msg")
                }
            }
            logger.log()
        }
    }
    if (logger != null) {
        logger.log("  【決着】 引き分け (手数上限 $maxSteps 手に到達)")
    }
    return KannPlayoutResult(
        winner = state.winner,
        turn = state.turn,
        steps = maxSteps,
        trajectory = trajectory,
        evaluatedCount = localEvaluated,
        visitedHashes = localVisited ?: emptySet()
    )
}

/**
 * 自己対戦(探索率 [exploreEpsilon])を全CPUコアで並列実行して序盤〜終盤のすべての状態(選択前・選択後)を記録し、
 * 最終結果(勝敗)に応じて [candidate] の重みを BPTT (誤差逆伝播) で直接誤差修正する学習フェーズ。
 */
private fun trainCandidateByBptt(
    net: KannValueNetwork,
    workerNets: List<KannValueNetwork>,
    candidate: FloatArray,
    baseline: FloatArray,
    trainGames: Int,
    rng: Random,
    maxSteps: Int,
    bpttLr: Float,
    exploreEpsilon: Double,
    deck1Name: String = "deck-kogyo.yaml",
    deck2Name: String = "deck-kogyo.yaml"
) {
    if (bpttLr <= 0f || trainGames <= 0) return
    val gameSeeds = LongArray(trainGames) { rng.nextLong() }
    val exploreSeeds = LongArray(trainGames) { rng.nextLong() }

    val results = runBlocking(Dispatchers.Default) {
        (0 until trainGames).map { i ->
            async {
                val workerNet = workerNets[i % workerNets.size]
                val candidateIsP1 = i % 2 == 0
                val nets = mapOf(1 to workerNet, 2 to workerNet)
                val weights = if (candidateIsP1) mapOf(1 to candidate, 2 to baseline) else mapOf(1 to baseline, 2 to candidate)
                val d1 = if (candidateIsP1) deck1Name else deck2Name
                val d2 = if (candidateIsP1) deck2Name else deck1Name
                playoutGameKann(
                    netsByPlayer = nets,
                    weightsByPlayer = weights,
                    seed = gameSeeds[i],
                    maxSteps = maxSteps,
                    deck1Name = d1,
                    deck2Name = d2,
                    recordTrajectory = true,
                    exploreEpsilon = exploreEpsilon,
                    rng = Random(exploreSeeds[i]),
                    parallelSafe = true
                )
            }
        }.awaitAll()
    }

    val pooledSamples = ArrayList<Pair<KannStateRecord, Double>>(trainGames * maxSteps)
    for (res in results) {
        EsDiag.totalSteps += res.steps
        EsDiag.evaluatedPositions += res.evaluatedCount
        EsDiag.distinctPositionsSeen.addAll(res.visitedHashes)
        for (rec in res.trajectory) {
            val label = when {
                res.winner == null -> 0.0
                res.winner == rec.playerId -> 1.0
                else -> 0.0
            }
            pooledSamples.add(rec to label)
        }
    }
    writeEsHeartbeat()

    if (pooledSamples.isEmpty()) return
    val shuffled = pooledSamples.shuffled(rng)
    net.setWeights(candidate)
    var costAccum = 0.0
    for ((rec, label) in shuffled) {
        costAccum += net.trainStep(rec.tokens, rec.globals, label, bpttLr)
    }
    val updated = net.getWeights()
    for (k in candidate.indices) candidate[k] = updated[k]
    val avgC = costAccum / shuffled.size
    println("  [BPTT誤差修正(並列)] 学習対局=$trainGames 局, 誤差修正状態数=${shuffled.size} (序盤〜終盤全状態), 平均誤差=${fmtMargin(avgC)}")
}

/**
 * 誤差修正済みの [candidate] 側の勝率を [baseline] との公平自己対戦(探索なし・重み固定)で全CPUコア並列測定する。
 * 先攻/後攻を交互に入れ替えて先手有利を打ち消す。
 * 手数上限引き分け(winner == null)は 0.0 点(両者敗北)として扱い、パス戦略を淘汰する。
 */
private fun evaluateCandidateKann(
    workerNets: List<KannValueNetwork>,
    candidate: FloatArray,
    baseline: FloatArray,
    games: Int,
    rng: Random,
    maxSteps: Int,
    deck1Name: String = "deck-kogyo.yaml",
    deck2Name: String = "deck-kogyo.yaml"
): Double {
    val gameSeeds = LongArray(games) { rng.nextLong() }
    val results = runBlocking(Dispatchers.Default) {
        (0 until games).map { i ->
            async {
                val workerNet = workerNets[i % workerNets.size]
                val candidateIsP1 = i % 2 == 0
                val nets = mapOf(1 to workerNet, 2 to workerNet)
                val weights = if (candidateIsP1) mapOf(1 to candidate, 2 to baseline) else mapOf(1 to baseline, 2 to candidate)
                val d1 = if (candidateIsP1) deck1Name else deck2Name
                val d2 = if (candidateIsP1) deck2Name else deck1Name
                playoutGameKann(
                    netsByPlayer = nets,
                    weightsByPlayer = weights,
                    seed = gameSeeds[i],
                    maxSteps = maxSteps,
                    deck1Name = d1,
                    deck2Name = d2,
                    recordTrajectory = false,
                    exploreEpsilon = 0.0,
                    parallelSafe = true
                )
            }
        }.awaitAll()
    }

    var score = 0.0
    var draws = 0
    for (i in 0 until games) {
        val res = results[i]
        val candidateIsP1 = i % 2 == 0
        EsDiag.gameIndex = i + 1
        EsDiag.stepInGame = res.steps.toLong()
        EsDiag.totalSteps += res.steps
        EsDiag.evaluatedPositions += res.evaluatedCount
        EsDiag.distinctPositionsSeen.addAll(res.visitedHashes)
        val winner = res.winner
        if (winner == null) draws++
        score += when {
            winner == null -> 0.0
            candidateIsP1 && winner == 1 -> 1.0
            !candidateIsP1 && winner == 2 -> 1.0
            else -> 0.0
        }
    }
    writeEsHeartbeat()
    if (draws > 0) {
        println("  (診断: 評価対局 $games 局中 $draws 局が引き分け=0.0点敗北扱い)")
    }
    return score / games
}

/**
 * 初期局面で「最良の行動」が「ステップ終了(END_STEP)」をどれだけ上回るかを測る。
 * 未学習の初期重みは高確率で END_STEP を常に他の全行動より高く評価してしまい
 * ([evaluateActionWithKann]で先読みした値がどれもEND_STEPに負ける)、
 * `best.second > kannStepEndEval(...)` ([playoutGameKann]) が常にFalseになって
 * 両者とも「常にパス」の固定方策に落ち、ESの重み変異が対局結果に反映されなくなる
 * (実測: sigma=0でも sigma=0.05でも毎世代ちょうど勝率50.0%に固着することを確認した)。
 * このマージンが正(いずれかの行動がEND_STEPを上回る)になる初期化を探すために使う。
 */
private fun endStepMargin(net: KannValueNetwork, state: GameState): Double {
    val actions = enumerateActions(state).filterNot { it.forbidden }
    if (actions.isEmpty()) return Double.NEGATIVE_INFINITY
    val bestV = actions.maxOf { evaluateActionWithKann(state, it, net) }
    return bestV - kannStepEndEval(state, net)
}

/**
 * KANNの重み初期化に使う大域RNG([kad_srand])のシードを何通りか試し、[endStepMargin]が
 * 最大(理想的には正)になる初期化を採用する。「ランダム初期重みで開始」というESの
 * 局所解対策として、学習前にこの探索を1回だけ行う。
 */
@OptIn(ExperimentalForeignApi::class)
private fun findGoodInitNet(trySeeds: Int, baseSeed: Long, config: KannArchConfig = KannArchConfig()): KannValueNetwork {
    val state = createInitialGameState()
    var bestNet: KannValueNetwork? = null
    var bestMargin = Double.NEGATIVE_INFINITY
    var bestSeed = baseSeed
    for (i in 0 until trySeeds) {
        val seed = baseSeed + i
        kad_srand(null, seed.toULong())
        val net = KannValueNetwork(config)
        val margin = endStepMargin(net, state)
        if (margin > bestMargin) {
            bestMargin = margin
            bestSeed = seed
            bestNet?.delete()
            bestNet = net
        } else {
            net.delete()
        }
    }
    println("初期重み探索: $trySeeds 通り中 seed=$bestSeed を採用 (END_STEP優位マージン=${fmtMargin(bestMargin)})")
    return bestNet!!
}

/** 小数第4位までの文字列にする (Kotlin/Native には String.format が無いため手書き) */
private fun fmtMargin(v: Double): String {
    val scaled = (v * 10000).toInt()
    val sign = if (scaled < 0) "-" else ""
    val a = kotlin.math.abs(scaled)
    return "$sign${a / 10000}.${(a % 10000).toString().padStart(4, '0')}"
}

/** 重み配列全体に独立な小さいガウスノイズを同時に加える ([RnnParams.mutate] と同じ発想) */
private fun mutateWeights(w: FloatArray, rng: Random, sigma: Float): FloatArray = FloatArray(w.size) { i ->
    val u1 = rng.nextDouble().coerceAtLeast(1e-12)
    val u2 = rng.nextDouble()
    val gaussian = kotlin.math.sqrt(-2.0 * kotlin.math.ln(u1)) * kotlin.math.cos(2.0 * kotlin.math.PI * u2)
    w[i] + (gaussian * sigma).toFloat()
}

/** 0.0〜1.0 を小数第1位までの % 文字列にする (Kotlin/Native には String.format が無いため手書き) */
private fun pct(v: Double): String {
    val hundredths = (v * 1000).toInt()
    return "${hundredths / 10}.${hundredths % 10}%"
}

/** [KannValueNetwork.save]([kann_save]形式)のファイルから重み配列だけを取り出す。グラフ構造は使い捨てる */
private fun loadWeightsFromFile(path: String): FloatArray {
    val loaded = KannValueNetwork.load(path) ?: error("重みファイルを読み込めません: $path")
    val w = loaded.getWeights()
    loaded.delete()
    return w
}

/**
 * パス文字列からモデル識別名（例: "w3", "w4", "init"）を抽出する。
 * 命名規則 `<派生元の名>_<派生先の名>.<NNタイプ>.weight` や既存ファイル名から自動判定する。
 */
fun extractModelName(path: String?): String {
    if (path == null) return "init"
    val filename = path.replace('\\', '/').substringAfterLast('/')
    val base = filename.substringBefore('.')
    if (base.contains('_')) {
        val dst = base.substringAfterLast('_')
        val clean = dst.substringBefore('-')
        val wMatch = Regex("""w\d+""", RegexOption.IGNORE_CASE).find(clean)
        if (wMatch != null) return wMatch.value.lowercase()
        val runMatch = Regex("""run\d+""", RegexOption.IGNORE_CASE).find(clean)
        if (runMatch != null) return "w" + runMatch.value.substring(3)
        return clean
    }
    val wMatch = Regex("""w\d+""", RegexOption.IGNORE_CASE).find(base)
    if (wMatch != null) return wMatch.value.lowercase()
    val runMatch = Regex("""run\d+""", RegexOption.IGNORE_CASE).find(base)
    if (runMatch != null) return "w" + runMatch.value.substring(3)
    if (base.contains("final", ignoreCase = true)) return "w1"
    return base
}

/**
 * 命名規則 `<派生元の名>_<派生先の名>.<NNタイプ>.weight` に基づくウェイトファイル名を生成する。
 * 出力先は `./weights/` ディレクトリに固定される。
 */
fun buildWeightFilename(srcName: String, dstName: String, nnType: String = "kann", cycleSuffix: String = ""): String {
    val suffix = if (cycleSuffix.isNotEmpty()) "-$cycleSuffix" else ""
    return "$WEIGHTS_DIR/${srcName}_${dstName}${suffix}.${nnType}.weight"
}

/**
 * `--kann-es-train` 起動時のエントリポイント。
 * `--generations` `--games` `--sigma` `--seed` `--max-steps` `--out` `--init-search` `--resume` `--cycles` で調整できる。
 *
 * `--resume <path>` を指定しない場合、学習前に [findGoodInitNet] で初期重みを探索する。
 * 未学習の初期重みのままだと高確率でEND_STEPが他の全行動より常に高く評価され、両者とも
 * 「常にパス」の固定方策に陥って重みを変異させても対局結果が一切変わらなくなる
 * (実測: 全世代がちょうど勝率50.0%に固着し学習が進まなかった)。初期重み探索でこの局所解を
 * 避けてから学習を始める。
 *
 * `--resume <path>` を指定した場合、そのファイル([KannValueNetwork.save]形式)を初期チャンピオン
 * として続きから学習する([findGoodInitNet]は実行しない)。
 * ウェイトファイル名は `<派生元の名>_<派生先の名>.<NNタイプ>.weight` 規則に従い自動生成される。
 */
fun runKannEsTraining(args: Array<String>) {
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

    val cycles = intArg("--cycles", 1)
    val generations = intArg("--generations", 20)
    val targetAdoptions = intArg("--target-adoptions", 0)
    val maxGenerations = intArg("--max-generations", if (targetAdoptions > 0) 500 else generations)
    val effectiveGenerations = if (targetAdoptions > 0 && !args.contains("--generations")) maxGenerations else generations
    val games = intArg("--games", 12)
    val sigma = doubleArg("--sigma", 0.02).toFloat()
    val bpttLr = doubleArg("--lr", 0.0005).toFloat()
    val exploreEpsilon = doubleArg("--epsilon", 0.15)
    val seed = intArg("--seed", 1)
    val maxSteps = intArg("--max-steps", 300)
    val initSearch = intArg("--init-search", 30)
    val deck1 = strArg("--deck1") ?: strArg("--deck") ?: "deck-kogyo.yaml"
    val deck2 = strArg("--deck2") ?: strArg("--deck") ?: "deck-kogyo.yaml"

    val resumeIdx = args.indexOf("--resume")
    val resumePath = if (resumeIdx >= 0 && resumeIdx + 1 < args.size) args[resumeIdx + 1] else null
    val nnType = strArg("--nn-type") ?: "kann"
    val srcName = strArg("--source-name") ?: extractModelName(resumePath)

    val hiddenDim = intArg("--hidden-dim", RNN_HIDDEN_DIM)
    val denseDim = intArg("--dense-dim", RNN_DENSE_DIM)
    val numDenseLayers = intArg("--dense-layers", 1)
    val archConfig = KannArchConfig(
        hiddenDim = hiddenDim,
        denseDim = denseDim,
        numDenseLayers = numDenseLayers
    )

    val targetArg = strArg("--target-name") ?: strArg("--name")
    val dstName = targetArg ?: run {
        val numMatch = Regex("""\d+""").find(srcName)
        if (numMatch != null) {
            val nextNum = (numMatch.value.toIntOrNull() ?: 0) + 1
            "w$nextNum"
        } else if (srcName == "init") {
            "w1"
        } else {
            "${srcName}_next"
        }
    }

    val outIdx = args.indexOf("--out")
    val outPath = if (outIdx >= 0 && outIdx + 1 < args.size) {
        val p = args[outIdx + 1]
        if (!p.contains('/') && !p.contains('\\')) "$WEIGHTS_DIR/$p" else p
    } else {
        buildWeightFilename(srcName, dstName, nnType)
    }

    // 並列実行前にメインスレッドでデッキ・カードマスタのキャッシュをウォームアップし、
    // ワーカースレッドからの読み取り専用アクセスを保証する
    CardLoader.loadDeck(deck1)
    CardLoader.loadDeck(deck2)

    val rng = Random(seed)
    val net: KannValueNetwork
    var champion: FloatArray
    if (resumePath != null) {
        val loaded = KannValueNetwork.load(resumePath)
            ?: error("重みファイルを読み込めません: $resumePath")
        net = loaded
        champion = net.getWeights()
    } else {
        net = findGoodInitNet(initSearch, seed.toLong() * 1000003L, archConfig)
        champion = net.getWeights()
    }

    val population = intArg("--population", 3).coerceAtLeast(1)
    val trainGames = intArg("--train-games", maxOf(2, games / 2))
    val poolSize = maxOf(population * games, population * trainGames, 12)
    val workerNets = List(poolSize) { net.cloneNet() }

    println("=== KANN価値ネットワーク: 全局面BPTT誤差修正 + 進化戦略(ES) マルチスレッド並列自己対戦学習開始 ===")
    val genDesc = if (targetAdoptions > 0) "目標改善数=$targetAdoptions 回 (上限 $effectiveGenerations 世代)" else "1サイクル世代数=$generations"
    println("パラメータ数=${net.nVar}, サイクル数=$cycles, $genDesc, 1世代候補数(λ)=$population, 1候補対戦数=$games (最大${poolSize}並列), 摂動幅=$sigma, BPTT学習率=$bpttLr, 探索率=$exploreEpsilon, 手数上限=$maxSteps")
    println(if (resumePath != null) "初期チャンピオン: $resumePath から続きを学習" else "初期チャンピオン: 新規探索(未学習)")
    println()

    for (cycle in 1..cycles) {
        if (cycles > 1) {
            println("=================================================================")
            println("=== 強化学習 サイクル $cycle / $cycles 開始 ===")
            println("=================================================================")
        }

        val cycleStartChampion = champion.copyOf()
        var adoptedCount = 0

        EsDiag.totalGenerations = effectiveGenerations
        EsDiag.games = games
        EsDiag.outPath = outPath

        for (gen in 1..effectiveGenerations) {
            EsDiag.generation = gen
            EsDiag.adoptedCount = adoptedCount

            val candidates = List(population) { mutateWeights(champion, rng, sigma) }

            // 1. 全候補(population × trainGames 局)の探索付き自己対戦を全CPUコアで一括並列実行し、
            //    序盤〜終盤の全状態と勝敗を収集して各候補をBPTT誤差修正する
            if (bpttLr > 0f && trainGames > 0) {
                val totalTrainTasks = population * trainGames
                val trainGameSeeds = LongArray(totalTrainTasks) { rng.nextLong() }
                val trainExploreSeeds = LongArray(totalTrainTasks) { rng.nextLong() }
                val trainResults = runBlocking(Dispatchers.Default) {
                    (0 until totalTrainTasks).map { taskIdx ->
                        async {
                            val candIdx = taskIdx / trainGames
                            val gameIdx = taskIdx % trainGames
                            val workerNet = workerNets[taskIdx % workerNets.size]
                            val cand = candidates[candIdx]
                            val candidateIsP1 = gameIdx % 2 == 0
                            val nets = mapOf(1 to workerNet, 2 to workerNet)
                            val weights = if (candidateIsP1) mapOf(1 to cand, 2 to champion) else mapOf(1 to champion, 2 to cand)
                            val d1 = if (candidateIsP1) deck1 else deck2
                            val d2 = if (candidateIsP1) deck2 else deck1
                            playoutGameKann(
                                netsByPlayer = nets,
                                weightsByPlayer = weights,
                                seed = trainGameSeeds[taskIdx],
                                maxSteps = maxSteps,
                                deck1Name = d1,
                                deck2Name = d2,
                                recordTrajectory = true,
                                exploreEpsilon = exploreEpsilon,
                                rng = Random(trainExploreSeeds[taskIdx]),
                                parallelSafe = true
                            )
                        }
                    }.awaitAll()
                }

                var totalTrainedStates = 0
                var costAccumAll = 0.0
                for (candIdx in 0 until population) {
                    val cand = candidates[candIdx]
                    val pooled = ArrayList<Pair<KannStateRecord, Double>>(trainGames * maxSteps)
                    for (gIdx in 0 until trainGames) {
                        val res = trainResults[candIdx * trainGames + gIdx]
                        EsDiag.totalSteps += res.steps
                        EsDiag.evaluatedPositions += res.evaluatedCount
                        EsDiag.distinctPositionsSeen.addAll(res.visitedHashes)
                        for (rec in res.trajectory) {
                            val label = when {
                                res.winner == null -> 0.0
                                res.winner == rec.playerId -> 1.0
                                else -> 0.0
                            }
                            pooled.add(rec to label)
                        }
                    }
                    if (pooled.isNotEmpty()) {
                        val shuffled = pooled.shuffled(rng)
                        net.resetRmsMemory()
                        net.setWeights(cand)
                        for ((rec, label) in shuffled) {
                            costAccumAll += net.trainStep(rec.tokens, rec.globals, label, bpttLr)
                        }
                        val updated = net.getWeights()
                        for (k in cand.indices) cand[k] = updated[k]
                        totalTrainedStates += shuffled.size
                    }
                }
                writeEsHeartbeat()
                val avgC = if (totalTrainedStates > 0) costAccumAll / totalTrainedStates else 0.0
                println("  [BPTT誤差修正(並列)] 候補数=$population, 学習対局=${population * trainGames} 局, 誤差修正状態数=$totalTrainedStates (序盤〜終盤全状態), 平均誤差=${fmtMargin(avgC)}")
            }

            // 2. 誤差修正済みの全候補(population × games 局)の評価自己対戦を全CPUコアで一括並列実行する
            val totalEvalTasks = population * games
            val evalSeeds = LongArray(totalEvalTasks) { rng.nextLong() }
            val evalResults = runBlocking(Dispatchers.Default) {
                (0 until totalEvalTasks).map { taskIdx ->
                    async {
                        val candIdx = taskIdx / games
                        val gameIdx = taskIdx % games
                        val workerNet = workerNets[taskIdx % workerNets.size]
                        val cand = candidates[candIdx]
                        val candidateIsP1 = gameIdx % 2 == 0
                        val nets = mapOf(1 to workerNet, 2 to workerNet)
                        val weights = if (candidateIsP1) mapOf(1 to cand, 2 to champion) else mapOf(1 to champion, 2 to cand)
                        val d1 = if (candidateIsP1) deck1 else deck2
                        val d2 = if (candidateIsP1) deck2 else deck1
                        playoutGameKann(
                            netsByPlayer = nets,
                            weightsByPlayer = weights,
                            seed = evalSeeds[taskIdx],
                            maxSteps = maxSteps,
                            deck1Name = d1,
                            deck2Name = d2,
                            recordTrajectory = false,
                            exploreEpsilon = 0.0,
                            parallelSafe = true
                        )
                    }
                }.awaitAll()
            }

            var bestCandIdx = 0
            var bestWinRate = -1.0
            val winRates = DoubleArray(population)
            for (candIdx in 0 until population) {
                var score = 0.0
                for (gIdx in 0 until games) {
                    val res = evalResults[candIdx * games + gIdx]
                    val candidateIsP1 = gIdx % 2 == 0
                    EsDiag.gameIndex = gIdx + 1
                    EsDiag.stepInGame = res.steps.toLong()
                    EsDiag.totalSteps += res.steps
                    EsDiag.evaluatedPositions += res.evaluatedCount
                    EsDiag.distinctPositionsSeen.addAll(res.visitedHashes)
                    val winner = res.winner
                    score += when {
                        winner == null -> 0.0
                        candidateIsP1 && winner == 1 -> 1.0
                        !candidateIsP1 && winner == 2 -> 1.0
                        else -> 0.0
                    }
                }
                val wr = score / games
                winRates[candIdx] = wr
                if (wr > bestWinRate) {
                    bestWinRate = wr
                    bestCandIdx = candIdx
                }
            }

            // 1次評価で勝ち越した候補は、独立シードの2次検証対戦(12局並列)でも勝ち越した場合のみ「真の最強ウェイト」として採用する(偶然の上振れによる劣化防止)
            var verifyWinRate = 0.0
            val adopted = if (bestWinRate > 0.5) {
                verifyWinRate = evaluateCandidateKann(
                    workerNets = workerNets,
                    candidate = candidates[bestCandIdx],
                    baseline = champion,
                    games = 12,
                    rng = rng,
                    maxSteps = maxSteps,
                    deck1Name = deck1,
                    deck2Name = deck2
                )
                verifyWinRate > 0.5
            } else {
                false
            }
            val prefix = if (cycles > 1) "[Cycle $cycle/$cycles] " else ""
            val allRatesStr = winRates.joinToString(", ") { pct(it) }
            val verifyStr = if (bestWinRate > 0.5) " (2次検証勝率=${pct(verifyWinRate)})" else ""
            println("${prefix}世代$gen: 候補勝率=[$allRatesStr] 最大=${pct(bestWinRate)}$verifyStr ${if (adopted) "→ 採用" else "→ 棄却"}")
            if (adopted) {
                champion = candidates[bestCandIdx]
                adoptedCount++
                net.setWeights(champion)
                net.save(outPath)
                if (targetAdoptions > 0 && adoptedCount >= targetAdoptions) {
                    println("=== 目標改善数 ($targetAdoptions 回) を達成したため学習を完了 ===")
                    break
                }
            }
            EsDiag.adoptedCount = adoptedCount
            writeEsHeartbeat(winRate = bestWinRate, adopted = adopted)
        }

        println()
        val cycleLabel = if (cycles > 1) "サイクル $cycle 完了" else "学習完了"
        println("=== $cycleLabel ($adoptedCount / ${EsDiag.generation} 世代で採用) ===")
        net.setWeights(champion)
        net.save(outPath)

        if (cycles > 1) {
            val snapshotPath = if (outPath.endsWith(".${nnType}.weight")) {
                val base = outPath.removeSuffix(".${nnType}.weight")
                "${base}-c${cycle}.${nnType}.weight"
            } else if (outPath.endsWith(".weight")) {
                val base = outPath.removeSuffix(".weight")
                "${base}-c${cycle}.weight"
            } else if (outPath.contains('.')) {
                val ext = outPath.substringAfterLast('.')
                val base = outPath.substringBeforeLast('.')
                "${base}-c${cycle}.${ext}"
            } else {
                "${outPath}-c${cycle}"
            }
            net.save(snapshotPath)
            println("=== スナップショット保存: $snapshotPath ===")
            println()
        }

        writeEsTrainingProgress(
            EsTrainingProgress(
                active = (cycle < cycles),
                generation = EsDiag.generation,
                totalGenerations = effectiveGenerations,
                games = games,
                winRate = null,
                adopted = null,
                adoptedCount = adoptedCount,
                outPath = outPath,
                updatedAtMs = currentTimeMs(),
                gameIndex = EsDiag.gameIndex,
                stepInGame = EsDiag.stepInGame,
                totalSteps = EsDiag.totalSteps,
                evaluatedPositions = EsDiag.evaluatedPositions,
                distinctPositionsSeen = EsDiag.distinctPositionsSeen.size.toLong()
            )
        )
    }

    if (cycles > 1) {
        println("=================================================================")
        println("=== 全 $cycles サイクル学習完了: 最終モデル保存 -> $outPath ===")
        println("=================================================================")
    } else {
        println("=== 学習済みモデルを保存: $outPath ===")
    }
    for (wn in workerNets) wn.delete()
    net.delete()
}

/**
 * `--kann-match` 起動時のエントリポイント。player1/player2 それぞれに独立した重み
 * ([KannValueNetwork.save]形式のファイル、`--p1-weights`/`--p2-weights`)を指定して
 * 対戦させ、勝敗を集計する。[playoutGameKann]は元々プレイヤーごとに異なる重み
 * ([weightsByPlayer]) を扱える設計だったが、これまで `--kann-es-train` の内部
 * (champion/candidate、いずれも同じ初期化由来)でしか使われていなかった。
 * このモードはそれをCLIから任意の2つの重みで使えるようにする。
 *
 * どちらか(または両方)を省略した場合は [findGoodInitNet] で探索した未学習の初期重みで
 * 代用する。`--games` `--max-steps` `--seed` `--init-search` で調整できる。
 */
fun runKannMatch(args: Array<String>) {
    fun intArg(flag: String, default: Int): Int {
        val idx = args.indexOf(flag)
        return if (idx >= 0 && idx + 1 < args.size) args[idx + 1].toIntOrNull() ?: default else default
    }
    fun strArg(flag: String): String? {
        val idx = args.indexOf(flag)
        return if (idx >= 0 && idx + 1 < args.size) args[idx + 1] else null
    }

    val games = intArg("--games", 10)
    val maxSteps = intArg("--max-steps", 300)
    val seed = intArg("--seed", 1)
    val initSearch = intArg("--init-search", 30)
    val p1Path = strArg("--p1-weights")
    val p2Path = strArg("--p2-weights")
    val deck1 = strArg("--deck1") ?: strArg("--deck") ?: "deck-kogyo.yaml"
    val deck2 = strArg("--deck2") ?: strArg("--deck") ?: "deck-kogyo.yaml"
    val alternate = !args.contains("--fixed-turn")

    val logArg = strArg("--log") ?: strArg("--log-file")
    val noLog = args.contains("--no-log")
    val logPath = if (noLog) null else (logArg ?: "kann-match.log")
    val logger = logPath?.let { MatchLogger(it) }

    val rng = Random(seed)
    val net1 = p1Path?.let { KannValueNetwork.load(it) } ?: findGoodInitNet(initSearch, seed.toLong() * 1000003L)
    val net2 = p2Path?.let { KannValueNetwork.load(it) } ?: findGoodInitNet(initSearch, seed.toLong() * 1000003L + 1L)

    val name1 = p1Path ?: "未学習初期重み(model1)"
    val name2 = p2Path ?: "未学習初期重み(model2)"

    println("=== KANN価値ネットワーク: 対戦評価モード ===")
    println("model1=$name1")
    println("model2=$name2")
    println("対戦数=$games, 先手後手入替=${if (alternate) "あり(公平交互)" else "なし(固定)"}, 手数上限=$maxSteps, 乱数シード=$seed")
    if (logger != null) {
        if (logger.open()) {
            println("対戦選択ログの出力先: $logPath")
            logger.log("=================================================================")
            logger.log("=== KANN価値ネットワーク 対戦評価 選択ログ ===")
            logger.log("=================================================================")
            logger.log("model1: $name1")
            logger.log("model2: $name2")
            logger.log("対戦数: $games, 先手後手入替: ${if (alternate) "あり(公平交互)" else "なし(固定)"}, 手数上限: $maxSteps, 乱数シード: $seed")
            logger.log()
        } else {
            println("警告: ログファイルを開けませんでした: $logPath")
        }
    }
    println()

    var wins1Total = 0
    var wins1AsP1 = 0
    var wins1AsP2 = 0
    var games1AsP1 = 0
    var games1AsP2 = 0

    var wins2Total = 0
    var wins2AsP1 = 0
    var wins2AsP2 = 0
    var games2AsP1 = 0
    var games2AsP2 = 0

    var draws = 0
    var totalTurns = 0
    var totalSteps = 0

    data class MatchRecord(
        val index: Int,
        val seed: Long,
        val winnerStr: String,
        val turn: Int,
        val steps: Int,
        val p1Name: String,
        val p2Name: String
    )
    val records = mutableListOf<MatchRecord>()

    for (g in 1..games) {
        val gameSeed = rng.nextLong()
        val m1IsP1 = if (alternate) (g % 2 != 0) else true
        val p1Name = if (m1IsP1) name1 else name2
        val p2Name = if (m1IsP1) name2 else name1

        if (m1IsP1) {
            games1AsP1++
            games2AsP2++
        } else {
            games1AsP2++
            games2AsP1++
        }

        if (logger != null) {
            logger.log("-----------------------------------------------------------------")
            logger.log("対戦 $g/$games [シード: $gameSeed]")
            logger.log("  先手(P1): $p1Name")
            logger.log("  後手(P2): $p2Name")
            logger.log("-----------------------------------------------------------------")
        }

        val d1 = if (m1IsP1) deck1 else deck2
        val d2 = if (m1IsP1) deck2 else deck1
        val playerNames = mapOf(1 to "P1($p1Name)", 2 to "P2($p2Name)")
        val netsByPlayer = if (m1IsP1) mapOf(1 to net1, 2 to net2) else mapOf(1 to net2, 2 to net1)
        val res = playoutGameKann(netsByPlayer, seed = gameSeed, maxSteps = maxSteps, logger = logger, playerNames = playerNames, deck1Name = d1, deck2Name = d2)
        totalTurns += res.turn
        totalSteps += res.steps

        val winnerModel = when (res.winner) {
            1 -> if (m1IsP1) 1 else 2
            2 -> if (m1IsP1) 2 else 1
            else -> null
        }

        when (winnerModel) {
            1 -> {
                wins1Total++
                if (m1IsP1) wins1AsP1++ else wins1AsP2++
            }
            2 -> {
                wins2Total++
                if (!m1IsP1) wins2AsP1++ else wins2AsP2++
            }
            else -> draws++
        }

        val winnerStr = when (winnerModel) {
            1 -> "$name1 (${if (m1IsP1) "先手" else "後手"})"
            2 -> "$name2 (${if (!m1IsP1) "先手" else "後手"})"
            else -> "引分"
        }

        records.add(
            MatchRecord(
                index = g,
                seed = gameSeed,
                winnerStr = winnerStr,
                turn = res.turn,
                steps = res.steps,
                p1Name = p1Name,
                p2Name = p2Name
            )
        )

        println("対戦 $g/$games [シード: $gameSeed]: 勝者=$winnerStr | 決着: ${res.turn}ターン / ${res.steps}手 (先手: $p1Name, 後手: $p2Name)")
    }

    net1.delete()
    net2.delete()

    val avgTurn = if (games > 0) (totalTurns.toDouble() / games) else 0.0
    val avgSteps = if (games > 0) (totalSteps.toDouble() / games) else 0.0

    fun fmtAvg(v: Double): String {
        val h = (v * 10).toInt()
        return "${h / 10}.${h % 10}"
    }

    println()
    println("=================================================================")
    println("=== 対戦評価集計結果 (全 $games 戦) ===")
    println("=================================================================")
    println("  $name1: ${pct(wins1Total.toDouble() / games)} (${wins1Total}勝)")
    if (games1AsP1 > 0) println("    - 先手時: ${wins1AsP1}勝 / ${games1AsP1}戦 (${pct(wins1AsP1.toDouble() / games1AsP1)})")
    if (games1AsP2 > 0) println("    - 後手時: ${wins1AsP2}勝 / ${games1AsP2}戦 (${pct(wins1AsP2.toDouble() / games1AsP2)})")
    println("  $name2: ${pct(wins2Total.toDouble() / games)} (${wins2Total}勝)")
    if (games2AsP1 > 0) println("    - 先手時: ${wins2AsP1}勝 / ${games2AsP1}戦 (${pct(wins2AsP1.toDouble() / games2AsP1)})")
    if (games2AsP2 > 0) println("    - 後手時: ${wins2AsP2}勝 / ${games2AsP2}戦 (${pct(wins2AsP2.toDouble() / games2AsP2)})")
    if (draws > 0) {
        println("  引き分け: ${pct(draws.toDouble() / games)} (${draws}局)")
    }
    println("  平均決着: ${fmtAvg(avgTurn)} ターン / ${fmtAvg(avgSteps)} 手")
    println()
    println("--- 各対戦の個別シード・結果一覧 ---")
    for (r in records) {
        println("  第${r.index.toString().padStart(2, ' ')}戦 [seed=${r.seed}]: 勝者=${r.winnerStr} (${r.turn}ターン / ${r.steps}手)")
    }
    println("=================================================================")

    if (logger != null) {
        logger.log()
        logger.log("=================================================================")
        logger.log("=== 対戦評価集計結果 (全 $games 戦) ===")
        logger.log("=================================================================")
        logger.log("  $name1: ${pct(wins1Total.toDouble() / games)} (${wins1Total}勝)")
        if (games1AsP1 > 0) logger.log("    - 先手時: ${wins1AsP1}勝 / ${games1AsP1}戦 (${pct(wins1AsP1.toDouble() / games1AsP1)})")
        if (games1AsP2 > 0) logger.log("    - 後手時: ${wins1AsP2}勝 / ${games1AsP2}戦 (${pct(wins1AsP2.toDouble() / games1AsP2)})")
        logger.log("  $name2: ${pct(wins2Total.toDouble() / games)} (${wins2Total}勝)")
        if (games2AsP1 > 0) logger.log("    - 先手時: ${wins2AsP1}勝 / ${games2AsP1}戦 (${pct(wins2AsP1.toDouble() / games2AsP1)})")
        if (games2AsP2 > 0) logger.log("    - 後手時: ${wins2AsP2}勝 / ${games2AsP2}戦 (${pct(wins2AsP2.toDouble() / games2AsP2)})")
        if (draws > 0) {
            logger.log("  引き分け: ${pct(draws.toDouble() / games)} (${draws}局)")
        }
        logger.log("  平均決着: ${fmtAvg(avgTurn)} ターン / ${fmtAvg(avgSteps)} 手")
        logger.log()
        logger.log("--- 各対戦の個別シード・結果一覧 ---")
        for (r in records) {
            logger.log("  第${r.index.toString().padStart(2, ' ')}戦 [seed=${r.seed}]: 勝者=${r.winnerStr} (${r.turn}ターン / ${r.steps}手)")
        }
        logger.log("=================================================================")
        logger.close()
        println("対戦選択ログを保存しました: $logPath")
    }
}
