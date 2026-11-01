package bstools.valuerl

import bstools.model.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.random.Random

/** 対局設定 */
data class GameConfig(
    /** 1局の手数上限 (超えたら引き分け)。Lv単位の列挙では1ターン約12手、デッキ切れは約70ターンなので余裕を持って1500 */
    val maxSteps: Int = 1500,
    val deck1: String = "deck-kogyo.yaml",
    val deck2: String = "deck-kogyo.yaml",
    val format: GameFormat = GameFormat.STANDARD
)

/**
 * 1局の結果。[trajectories] はプレイヤーごとの afterstate 特徴列 (そのプレイヤーが手を選んだ直後の局面、
 * そのプレイヤー視点)。決着した手の afterstate は値が確定(±1)しているので記録しない。
 * [searchValues] は各 afterstate について、手を選んだときの探索値 (そのプレイヤー視点)。
 */
class GameResult(
    val winner: Int?,
    val turns: Int,
    val steps: Int,
    val trajectories: Map<Int, List<Features>>,
    val searchValues: Map<Int, List<Double>> = emptyMap()
)

/** ファイルへの対局ログ */
@OptIn(ExperimentalForeignApi::class)
class GameLog(path: String) {
    private val f = fopen(path, "w")
    fun line(s: String = "") { f?.let { fputs(s + "\n", it) } }
    fun close() { f?.let { fclose(it) } }
}

fun playGame(
    agents: Map<Int, Agent>,
    seed: Long,
    cfg: GameConfig,
    rng: Random,
    record: Boolean = false,
    log: GameLog? = null
): GameResult {
    val state = createInitialGameState(cfg.deck1, cfg.deck2, seed, cfg.format)
    state.visitedPositions.add(positionHash(state))
    val traj = mapOf(1 to ArrayList<Features>(), 2 to ArrayList<Features>())
    val searched = mapOf(1 to ArrayList<Double>(), 2 to ArrayList<Double>())
    var step = 0
    while (state.winner == null && step < cfg.maxSteps) {
        val me = state.choosingPlayerId
        val agent = agents.getValue(me)
        val choice = agent.choose(state, rng)
        log?.let { lg ->
            lg.line("T${state.turn} #$step P$me(${agent.name}) ${state.step.displayName} | " +
                "Life ${state.player1.life}-${state.player2.life} Hand ${state.player1.hand.size}-${state.player2.hand.size} " +
                "Field ${state.player1.field.size}-${state.player2.field.size}")
            for ((d, v) in choice.alternatives) lg.line("    ${fmt(v)}  $d")
            lg.line("  => ${choice.action?.detail ?: "ステップ終了"}" + (choice.value?.let { " (${fmt(it)})" } ?: ""))
        }
        val msgs = mutableListOf<String>()
        val a = choice.action
        if (a == null) {
            advanceStep(state, msgs)
            state.visitedPositions.add(positionHash(state))
        } else {
            applyActionAndRecord(state, a, msgs)
        }
        if (record && !choice.forced && state.winner == null) {
            traj.getValue(me).add(extractFeatures(state, me))
            searched.getValue(me).add(choice.value ?: 0.0)
        }
        step++
    }
    log?.line("結果: " + (state.winner?.let { "P$it 勝利" } ?: "引き分け(手数上限)") + " / ${state.turn}ターン / ${step}手")
    return GameResult(state.winner, state.turn, step, traj, searched)
}

/** 対戦成績 (A 視点) */
class ArenaStats(var aWins: Int = 0, var bWins: Int = 0, var draws: Int = 0,
                 var aFirstWins: Int = 0, var aFirstGames: Int = 0, var turnSum: Long = 0, var stepSum: Long = 0) {
    val games get() = aWins + bWins + draws
    /** 引き分けは0.5勝 */
    val score get() = if (games == 0) 0.0 else (aWins + 0.5 * draws) / games
}

/**
 * 先手・後手を1局ごとに交互に入れ替えて A vs B を対戦させる。
 * 各局は並列に指す ([playGamesParallel])。ログを取るときだけ順番に指す (ログの書き込みを混ぜないため)。
 */
fun arena(a: Agent, b: Agent, games: Int, seed: Long, cfg: GameConfig, log: GameLog? = null): ArenaStats {
    val rng = Random(seed)
    val st = ArenaStats()
    val seeds = List(games) { rng.nextLong() to rng.nextLong() }
    val results: List<GameResult> = if (log == null) {
        playGamesParallel(seeds, cfg, record = false) { g -> if (g % 2 == 0) a to b else b to a }
    } else {
        seeds.mapIndexed { g, (boardSeed, choiceSeed) ->
            val agents = if (g % 2 == 0) mapOf(1 to a, 2 to b) else mapOf(1 to b, 2 to a)
            log.line("===== 第${g + 1}局 P1=${agents.getValue(1).name} P2=${agents.getValue(2).name} =====")
            playGame(agents, boardSeed, cfg, Random(choiceSeed), log = log)
        }
    }
    for ((g, r) in results.withIndex()) {
        val aFirst = g % 2 == 0
        val aId = if (aFirst) 1 else 2
        if (aFirst) st.aFirstGames++
        when (r.winner) {
            null -> st.draws++
            aId -> { st.aWins++; if (aFirst) st.aFirstWins++ }
            else -> st.bWins++
        }
        st.turnSum += r.turns
        st.stepSum += r.steps
    }
    return st
}

// ---- Kotlin/Native には String.format が無いので手書き ----

fun fmt(v: Double, digits: Int = 4): String {
    var scale = 1L
    repeat(digits) { scale *= 10 }
    val s = kotlin.math.round(v * scale).toLong()
    val sign = if (s < 0) "-" else ""
    val a = kotlin.math.abs(s)
    if (digits == 0) return "$sign$a"
    return "$sign${a / scale}.${(a % scale).toString().padStart(digits, '0')}"
}

fun pct(v: Double): String = fmt(v * 100, 1) + "%"
