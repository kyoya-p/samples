import bstools.model.*
import bstools.valuerl.*
import kotlinx.cinterop.*
import platform.posix.*

/**
 * Playmat 用の評価器の状態。BSToolsCla の GRU 価値ネットワーク (weights フォルダの .gru.weight) を
 * player1 / player2 それぞれに割り当てる (片方だけ指定した場合はもう片方と共有)。
 *
 * 評価方法は学習時の NetAgent と同じ: 各選択肢を [depth] 手先までミニマックスで読み、選ぶ側の視点で値を出す
 * (depth=1 は仮に適用した局面の V 評価。決着する手は勝ち +1 / 負け −1)。ルールエンジン自体は評価値を付けない。
 */
object PlaymatEval {
    const val WEIGHTS_DIR = "weights"
    const val DEFAULT_WEIGHTS = "g1_g2-best.gru.weight"

    var netP1: GruValueNet? = null
    var netP2: GruValueNet? = null
    var nameP1: String? = null
    var nameP2: String? = null
    /**
     * 選択肢の列挙 (`--actions`): level (既定) = Lv単位の正規形列挙 (学習の既定と同じ) /
     * all = すべての合法手 (一括再配置を含む。終盤は数万件以上になる) / compact = 圧縮列挙
     */
    var actions: String = "level"
        set(v) { field = v; configChanged() }
    /** ミニマックス探索の深さ (1 = 1手先読み)。起動時の --depth か /api/eval-config の "depth" で変更 */
    var depth: Int = 1
        set(v) { field = v; configChanged() }
    /** 探索の幅 (0 = 制限なし)。深さ2以上で、各局面で深く読む手を1手先読みの上位 width 手に絞る */
    var width: Int = 1
        set(v) { field = v; configChanged() }

    val mode: String get() = if (netP1 == null && netP2 == null) "NONE" else "GRU"

    fun netFor(playerId: Int): GruValueNet? = if (playerId == 1) (netP1 ?: netP2) else (netP2 ?: netP1)

    fun path(name: String): String = if (name.contains('/') || name.contains('\\')) name else "$WEIGHTS_DIR/$name"

    /** @return "ok" または表示用のエラーメッセージ */
    fun configure(mode: String, p1: String?, p2: String?): String {
        when (mode.uppercase()) {
            "NONE" -> { netP1 = null; netP2 = null; nameP1 = null; nameP2 = null }
            "GRU" -> {
                val n1 = p1?.takeIf { it.isNotBlank() }
                val n2 = p2?.takeIf { it.isNotBlank() }
                val first = n1 ?: n2 ?: DEFAULT_WEIGHTS
                val l1 = GruValueNet.load(path(n1 ?: first)) ?: return "player1の重みファイルを読み込めません: ${n1 ?: first}"
                val l2 = if (n2 == null) null else (GruValueNet.load(path(n2)) ?: return "player2の重みファイルを読み込めません: $n2")
                netP1 = l1; nameP1 = n1 ?: first
                netP2 = l2; nameP2 = n2
            }
            else -> return "不明な評価方式: $mode (GRU / NONE)"
        }
        configChanged()
        return "ok"
    }

    /** weights/ 配下の *.gru.weight を列挙する */
    @OptIn(ExperimentalForeignApi::class)
    fun listWeightFiles(): List<String> {
        val out = mutableListOf<String>()
        val dir = opendir(WEIGHTS_DIR) ?: return out
        try {
            while (true) {
                val e = readdir(dir) ?: break
                val name = e.pointed.d_name.toKString()
                if (name.endsWith(".gru.weight")) out.add(name)
            }
        } finally {
            closedir(dir)
        }
        return out.sorted()
    }

    /**
     * 評価値なしの選択肢。index は [listActions] と一致する (適用時の照合用)。
     * 既定は学習の既定と同じ Lv単位の正規形列挙 ([enumerateLevelActions]):
     *  - プレイ: リザーブ払い、Lvの境目の数のコアを置く (ソウルコアを含める / 含めない)
     *  - Lv変更: 各カードのコアをLvの境目の数にする / ソウルコアの移動 (どちらもプレイ等の間に1回ずつ)
     * `--actions all` はすべての合法手 (一括再配置を含む)、`--actions compact` は圧縮列挙。
     */
    fun rawActions(state: GameState): List<GameAction> = when (actions) {
        "all" -> enumerateAllActions(state)
        "compact" -> enumerateCompactActions(state)
        else -> enumerateLevelActions(state)
    }

    /**
     * 評価の進み具合の通知先 (評価済みの選択肢数, 現局面の全選択肢数)。サーバーが SSE で画面へ送る。
     * 深さ2以上では1局面の評価に時間がかかるため、途中経過を「選択肢数:x/X」として見せる
     */
    var progress: ((done: Int, total: Int) -> Unit)? = null

    /** 評価器・探索設定が変わったら増やす (評価結果のキャッシュを無効にする) */
    private var configVersion = 0
    fun configChanged() { configVersion++ }

    /** 1局面分の評価結果 (選択肢の評価値とステップ終了の評価値)。同じ局面を視点ごと・リクエストごとに評価し直さない */
    private class Evaluated(val key: String, val actions: List<GameAction>, val stepEnd: Double?, val total: Int)
    private var cache: Evaluated? = null

    private fun keyOf(state: GameState): String =
        "$configVersion:${positionHash(state)}:${state.turn}:${state.step}:${state.choosingPlayerId}:${state.coreWindow.sorted()}:" +
            "${state.visitedPositions.size}:${state.visitedPositions.fold(0L) { a, b -> a xor b }}"

    /**
     * 現局面の選択肢をすべて評価する (同一局面に戻る手を除く全選択肢 + ステップ終了)。
     * 1つ評価するごとに [progress] へ (評価済み数, 全選択肢数) を通知する (100ms おき + 最後)。
     */
    private fun evaluateAll(state: GameState): Evaluated {
        val key = keyOf(state)
        cache?.let { if (it.key == key) return it }
        val acts = rawActions(state)
        val net = netFor(state.choosingPlayerId)
        val canEnd = state.winner == null && state.step != Step.BLOCK_DECLARATION
        if (net == null || state.winner != null) {
            return Evaluated(key, acts, null, 0).also { cache = it }
        }
        val targets: List<GameAction?> = acts.filterNot { it.repeatsPosition } + (if (canEnd) listOf(null) else emptyList())
        val agent = NetAgent(net, depth = depth, width = width)
        val values = DoubleArray(targets.size)
        val clock = kotlin.time.TimeSource.Monotonic
        var last = clock.markNow()
        progress?.invoke(0, targets.size)
        if (depth <= 1) {
            // 1手先読みはまとめて評価したほうが速い (1局面あたり約0.1ms) ので一括で行う
            agent.evaluateOptions(state, targets).copyInto(values)
        } else {
            for (i in targets.indices) {
                values[i] = agent.evaluateOptions(state, listOf(targets[i]))[0]
                if (last.elapsedNow().inWholeMilliseconds >= 100) { progress?.invoke(i + 1, targets.size); last = clock.markNow() }
            }
        }
        progress?.invoke(targets.size, targets.size)
        val byIndex = HashMap<Int, Double>()
        var stepEnd: Double? = null
        for ((i, a) in targets.withIndex()) if (a == null) stepEnd = values[i] else byIndex[a.index] = values[i]
        val evaluated = acts.map { it.copy(eval = byIndex[it.index] ?: -1.0) }
        return Evaluated(key, evaluated, stepEnd, targets.size).also { cache = it }
    }

    /** 表示・適用に使う選択肢 (eval 付き)。同一局面に戻る手は -1 にして自動選択から外す */
    fun listActions(state: GameState): List<GameAction> = evaluateAll(state).actions

    /**
     * 現局面で評価する選択肢の数 (同一局面に戻る手以外 + ステップ終了)。評価器がない (評価なし) ときは 0。
     * 探索の深さが2以上でも、数えるのは現局面の選択肢だけ (その先の局面の選択肢は数えない)
     */
    fun evaluatedOptionCount(state: GameState): Int = evaluateAll(state).total

    /** ステップ終了の評価値 (選択肢と同じ評価器・同じ視点) */
    fun stepEndValue(state: GameState): Double = evaluateAll(state).stepEnd ?: 0.0
}
