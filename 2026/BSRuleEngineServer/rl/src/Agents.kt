package bstools.valuerl

import bstools.model.*
import kotlin.random.Random

/**
 * 着手を決めるエージェント。[choose] は「適用する手」を返し、null は「ステップ終了」を意味する。
 * 盤面への適用は [playGame] 側が一括で行う (エージェントは state を変更しない)。
 */
interface Agent {
    val name: String
    fun choose(state: GameState, rng: Random): Choice
}

/**
 * 選んだ手と、その手の評価値 (ログ用、無ければ null)。
 * [forced] は選択の余地が無かった (選べるのがステップ終了だけ等) ことを示し、学習の軌跡には記録しない
 * (bs.quensonwai.com の autoPass と同じ考え方: 意味のある選択が無い局面は自動で進める)。
 */
class Choice(val action: GameAction?, val value: Double? = null,
             val alternatives: List<Pair<String, Double>> = emptyList(), val forced: Boolean = false)

/** 選択肢の列挙方法 (`--actions`) */
enum class ActionSet {
    /**
     * Lv単位の正規形列挙 ([enumerateLevelActions]、既定)。プレイはリザーブ払い・Lvの境目の配置、
     * コアは「カードのLv変更」と「ソウルコアの移動」(各対象はプレイ等の間に1回)。
     * 各カードのLvとソウルコアの場所のすべての組み合わせに到達でき、候補数は小さく循環しない
     */
    LEVEL,
    /**
     * すべての合法手 ([enumerateAllActions])。プレイの支払い・配置の全組み合わせと、コア配置の全組み合わせ (一括再配置)。
     * 終盤は配置の数が数万〜数十万になるため学習には向かない
     */
    ALL,
    /** 圧縮列挙 ([enumerateCompactActions])。支払いは既定ルール1通り、コア移動は意味のある再配置だけ */
    COMPACT,
    /** 従来の全列挙 ([enumerateActions])。件数に上限あり (プレイは配置数ごとに50、コア移動は120) */
    FULL;

    companion object {
        fun parse(s: String?): ActionSet = when (s?.lowercase()) {
            "all" -> ALL
            "compact" -> COMPACT
            "full" -> FULL
            else -> LEVEL
        }
    }
}

/** 全スレッド共通 (対局の並列実行でも同じ列挙を使う)。起動時に1回だけ設定する */
var actionSet: ActionSet = ActionSet.LEVEL

/** 現局面で「選べる手」(禁止手・同一局面に戻る手を除く) */
fun legalCandidates(state: GameState): List<GameAction> =
    when (actionSet) {
        ActionSet.LEVEL -> enumerateLevelActions(state)
        ActionSet.ALL -> enumerateAllActions(state)
        ActionSet.COMPACT -> enumerateCompactActions(state)
        ActionSet.FULL -> enumerateActions(state)
    }.filterNot { it.forbidden || it.repeatsPosition }

/** ブロック宣言では「ステップ終了」は選べない (代わりに PASS_BLOCK が列挙される) */
fun canEndStep(state: GameState): Boolean = state.step != Step.BLOCK_DECLARATION

/** 手(null=ステップ終了)を試しに適用した後の局面 */
fun afterstate(state: GameState, action: GameAction?): GameState {
    val trial = state.snapshot()
    val msgs = mutableListOf<String>()
    if (action == null) advanceStep(trial, msgs) else applyAction(trial, action, msgs)
    return trial
}

/** 現局面の選択肢 (候補手 + 可能ならステップ終了)。null はステップ終了 */
fun optionsOf(state: GameState): List<GameAction?> {
    val o: MutableList<GameAction?> = legalCandidates(state).toMutableList()
    if (canEndStep(state) || o.isEmpty()) o.add(null)
    return o
}

/**
 * 価値ネットワークによる [depth] 手先までのミニマックス探索 (depth=1 は従来の1手先読み = afterstate 評価)。
 * すべての候補とステップ終了を**同じ評価器**で比較する (基準値の不一致を構造的に排除)。
 * 決着する手は評価器を通さず ±1 を与える。
 *
 * 深さは「選択の余地がある局面」の数で数える。手番がどちらにあるかは問わず、
 * 自分が選ぶ局面では最大、相手が選ぶ局面では最小を取る (αβ枝刈り付き)。
 * 選択肢が1つしかない局面 (自動進行) は深さを消費せずに進める。
 * 末端の評価は「その手を選んだプレイヤーの視点」で行い、相手の視点なら符号を反転する
 * (学習データの afterstate と同じ視点で評価するため)。
 *
 * 探索: 確率 [epsilon] で一様ランダム、それ以外は [temperature] > 0 ならソフトマックス、0 なら貪欲。
 * 温度は [tempTurns] ターン目までだけ使い、それ以降は貪欲に指す (序盤だけ展開を散らす用途)。
 * ルートの各候補は個別に正確な値を求める (温度付き抽選とログに全候補の値が要るため、ルートでは枝刈りしない)。
 *
 * 幅 [width] (既定1 = 最善の1手だけ深く読む、0 = 制限なし): 深さ2以上で、各局面で展開する選択肢を1手先読みの値の上位 [width] 手に絞る
 * (ビーム探索)。上位の外にある良い手は読み落とすが、評価局面数はおおむね width^(depth-1) × 候補数に抑えられる。
 */
class NetAgent(
    val net: GruValueNet,
    override val name: String = "gru",
    var epsilon: Double = 0.0,
    var temperature: Double = 0.0,
    var tempTurns: Int = Int.MAX_VALUE,
    var depth: Int = 1,
    var width: Int = 1
) : Agent {
    /** 価値ネットで評価した局面の累計 (計測用) */
    var evalCount = 0L

    override fun choose(state: GameState, rng: Random): Choice {
        val options = optionsOf(state)
        if (options.size == 1) return Choice(options[0], forced = true)

        val values = evaluateOptions(state, options)

        val pick = when {
            options.size == 1 -> 0
            epsilon > 0 && rng.nextDouble() < epsilon -> rng.nextInt(options.size)
            temperature > 0 && state.turn <= tempTurns -> softmaxSample(values, temperature, rng)
            else -> values.indices.maxBy { values[it] }
        }
        return Choice(
            options[pick], values[pick],
            options.indices.map { (options[it]?.detail ?: "ステップ終了") to values[it] }
        )
    }

    /**
     * 各選択肢 (null=ステップ終了) の [depth] 手先までの探索値 (選ぶプレイヤー視点)。
     * [width] で幅を制限した場合、深く読むのは1手先読みの値の上位 [width] 手だけ。
     * 残りの手は1手先読みの値を、深く読んだ手の最小値より小さくなるよう切り詰めて返す
     * (深さの違う値を比べて、読んでいない手が選ばれるのを防ぐ)。
     */
    fun evaluateOptions(state: GameState, options: List<GameAction?>): DoubleArray {
        val me = state.choosingPlayerId
        state.evaluatedCount += options.size
        if (depth <= 1) return leafValues(state, options, me)
        fun deep(i: Int): Double {
            val after = afterstate(state, options[i])
            val winner = after.winner
            return if (winner != null) (if (winner == me) 1.0 else -1.0) else search(after, depth - 1, me, -2.0, 2.0)
        }
        if (width <= 0 || options.size <= width) return DoubleArray(options.size) { deep(it) }
        val shallow = leafValues(state, options, me)
        val top = options.indices.sortedByDescending { shallow[it] }.take(width)
        val values = shallow.copyOf()
        for (i in top) values[i] = deep(i)
        val floor = top.minOf { values[it] } - 1e-6
        val expanded = top.toSet()
        for (i in options.indices) if (i !in expanded) values[i] = minOf(shallow[i], floor)
        return values
    }

    /**
     * 探索の途中の局面で展開する選択肢の順番。幅の制限がなければ全部 (列挙順)。
     * 制限があれば1手先読みの値で並べ、手番側にとって良い順に [width] 手だけ (αβ枝刈りも効きやすくなる)。
     */
    private fun expandOrder(s: GameState, options: List<GameAction?>, me: Int, maximize: Boolean): List<Int> {
        if (width <= 0 || options.size <= width) return options.indices.toList()
        val v = leafValues(s, options, me)
        return options.indices.sortedBy { if (maximize) -v[it] else v[it] }.take(width)
    }

    /**
     * 局面 [s] の各選択肢を1手適用した局面の値 ([me] 視点)。決着は ±1、それ以外は価値ネットでまとめて評価する。
     * 評価は選んだプレイヤー (s の手番) の視点で行い、[me] でなければ符号を反転する。
     */
    private fun leafValues(s: GameState, options: List<GameAction?>, me: Int): DoubleArray {
        val chooser = s.choosingPlayerId
        val values = DoubleArray(options.size)
        val pending = ArrayList<Features>()
        val pendingIdx = ArrayList<Int>()
        for ((i, a) in options.withIndex()) {
            val after = afterstate(s, a)
            val winner = after.winner
            if (winner != null) {
                values[i] = if (winner == me) 1.0 else -1.0
            } else {
                pending.add(extractFeatures(after, chooser))
                pendingIdx.add(i)
            }
        }
        evalCount += pending.size
        val evals = net.evaluateBatch(pending)
        val sign = if (chooser == me) 1.0 else -1.0
        for ((k, i) in pendingIdx.withIndex()) values[i] = sign * evals[k]
        return values
    }

    /** αβ探索。[s] は未決着、[d] ≥ 1 は残りの深さ。戻り値は [me] 視点の値 */
    private fun search(s0: GameState, d: Int, me: Int, alpha0: Double, beta0: Double): Double {
        // 選択の余地がない局面は深さを消費せずに進める (ステップ終了だけ等)
        var s = s0
        var options = optionsOf(s)
        var guard = 0
        while (options.size == 1 && guard++ < FORCED_LIMIT) {
            s = afterstate(s, options[0])
            s.winner?.let { return if (it == me) 1.0 else -1.0 }
            options = optionsOf(s)
        }
        val maximize = s.choosingPlayerId == me
        if (d == 1) {
            val v = leafValues(s, options, me)
            return if (maximize) v.max() else v.min()
        }
        var alpha = alpha0
        var beta = beta0
        var best = if (maximize) -2.0 else 2.0
        for (i in expandOrder(s, options, me, maximize)) {
            val after = afterstate(s, options[i])
            val winner = after.winner
            val v = if (winner != null) (if (winner == me) 1.0 else -1.0) else search(after, d - 1, me, alpha, beta)
            if (maximize) { if (v > best) best = v; if (best > alpha) alpha = best }
            else { if (v < best) best = v; if (best < beta) beta = best }
            if (alpha >= beta) break
        }
        return best
    }

    private fun softmaxSample(v: DoubleArray, t: Double, rng: Random): Int {
        val max = v.max()
        val p = DoubleArray(v.size) { kotlin.math.exp((v[it] - max) / t) }
        var r = rng.nextDouble() * p.sum()
        for (i in p.indices) { r -= p[i]; if (r <= 0) return i }
        return p.size - 1
    }

    companion object {
        /** 探索中に自動進行で続けて進める最大手数 (無限ループ防止) */
        const val FORCED_LIMIT = 64
    }
}

/** 一様ランダム方策。評価を一切持たない最弱の基準 (学習の進捗確認用の対戦相手) */
class RandomAgent(override val name: String = "random") : Agent {
    override fun choose(state: GameState, rng: Random): Choice {
        val options = optionsOf(state)
        return Choice(options[rng.nextInt(options.size)])
    }
}

/**
 * 探索の設定 (深さ・幅) の検査。問題があれば理由を返す。
 * 幅1は「1手先読みの1位の手だけを深く読む」設定で、読まなかった手は選ばれないため、
 * 深さを2以上にしても選ぶ手は深さ1と変わらない (深さの指定が無意味)。誤った設定を黙って受け入れないよう、
 * 幅1のときは深さ1以外を許さない。深さ2以上で読むには幅を2以上 (または 0 = 制限なし) にする。
 */
fun searchSettingsError(depth: Int, width: Int): String? = when {
    depth < 1 -> "--depth は1以上にしてください (指定: $depth)"
    width < 0 -> "--width は0以上にしてください (0 = 制限なし、指定: $width)"
    width == 1 && depth != 1 ->
        "--width 1 のとき --depth は1のみ指定できます (指定: --depth $depth)。幅1では1手先読みの1位の手しか選ばれず、" +
            "深さを上げても選ぶ手が変わりません。深さ2以上で読むには --width を2以上 (または 0 = 制限なし) にしてください"
    else -> null
}
