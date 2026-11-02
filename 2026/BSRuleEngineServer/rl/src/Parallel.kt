package bstools.valuerl

import bstools.model.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlin.native.concurrent.*
import kotlin.random.Random

/**
 * 対局の並列実行。自己対戦・評価対局・リーグ戦の各局は独立なので、スレッド (Kotlin/Native の Worker) に分けて同時に指す。
 *
 * 安全性:
 *  - 価値ネットの評価は重みを読むだけで、作業用の配列は呼び出しごとに確保する (共有して問題ない)。
 *    重みを更新するのは全局が終わった後 (Trainer.update) だけ
 *  - エージェントは評価局面数などの可変状態を持つので、局ごとに [threadCopy] で複製して使う
 *  - カード・デッキの読み込みキャッシュは、並列化の前に [warmUp] で読み込んでおき、以降は読むだけにする
 *  - 乱数は局ごとに種を事前に決めて独立に作る (スレッド数によらず同じ結果になる)
 */
@OptIn(ObsoleteWorkersApi::class, ExperimentalForeignApi::class)
object Parallel {
    /** 同時に指す局数 (既定: 論理プロセッサ数)。`--threads` で変更 */
    var threads: Int = defaultThreads()

    private var pool: List<Worker> = emptyList()

    private fun defaultThreads(): Int =
        (platform.posix.getenv("NUMBER_OF_PROCESSORS")?.toKString()?.toIntOrNull() ?: 4).coerceIn(1, 64)

    private fun workers(): List<Worker> {
        if (pool.size != threads) {
            pool.forEach { it.requestTermination().result }
            pool = List(threads) { Worker.start(name = "game-$it") }
        }
        return pool
    }

    /** デッキとカードを読み込んでキャッシュを埋める (並列実行中にキャッシュへ書き込まないようにする) */
    fun warmUp(cfg: GameConfig) {
        createInitialGameState(cfg.deck1, cfg.deck2, 1, cfg.format)
    }

    /** [items] の各要素に [f] を並列に適用し、元の順番で結果を返す */
    fun <T, R> map(items: List<T>, f: (T) -> R): List<R> {
        if (threads <= 1 || items.size <= 1) return items.map(f)
        val ws = workers()
        val jobs = items.map { item -> { f(item) } }
        val futures = jobs.mapIndexed { i, job ->
            ws[i % ws.size].execute(TransferMode.SAFE, { job }) { it() }
        }
        return futures.map { it.result }
    }
}

/** 並列実行用にエージェントを複製する (可変状態を局ごとに分けるため) */
fun Agent.threadCopy(): Agent = when (this) {
    is NetAgent -> NetAgent(net, name, epsilon, temperature, tempTurns, depth, width)
    else -> this
}

/**
 * 複数局を並列に指す。[seeds] の各要素が1局 (盤面の種と、手の選択の乱数の種)。
 * [agentsFor] は局番号から (P1, P2) のエージェントを返す (中で [threadCopy] される)。
 */
fun playGamesParallel(
    seeds: List<Pair<Long, Long>>,
    cfg: GameConfig,
    record: Boolean,
    agentsFor: (Int) -> Pair<Agent, Agent>
): List<GameResult> {
    Parallel.warmUp(cfg)
    val tasks = seeds.indices.map { g ->
        val (a1, a2) = agentsFor(g)
        Triple(seeds[g], a1.threadCopy(), a2.threadCopy())
    }
    return Parallel.map(tasks) { (seed, a1, a2) ->
        playGame(mapOf(1 to a1, 2 to a2), seed.first, cfg, Random(seed.second), record = record)
    }
}
