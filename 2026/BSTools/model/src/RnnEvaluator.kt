package bstools.model

import kotlin.math.exp
import kotlin.math.tanh
import kotlin.native.concurrent.ThreadLocal
import kotlin.random.Random
import kotlinx.cinterop.*
import platform.posix.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.decodeFromByteArray

/**
 * RNN(GRU)による局面評価器。
 *
 * これまでの `EvalWeights` は行動タイプごとに手書きした線形式で、隠れ層は存在しなかった。
 * ここでは「手札(自分)→自分の場→相手の場」という**可変長のカード列**を GRU で1つの
 * 隠れベクトルに集約し、ライフ等のスカラー特徴と結合してから2層の全結合で1スカラーに
 * 落とす、局面価値関数 V(state) を実装する。
 *
 * 選択肢1件の評価値は、「その手を仮に適用した後の局面」を V(state) で評価することで得る
 * (1手先読み)。これにより、行動タイプごとに別々の式を書く必要がなくなり、
 * 召喚・コア移動・アタック・ブロックのすべてを同じ評価器で扱える。
 *
 * Kotlin/Native にはオートグラッド(自動微分)ライブラリが無いため、学習は Tuner.kt と同じ
 * 自己対戦 + 進化戦略(ES)で行う。ただしパラメータが約1780個あるため、EvalWeights のような
 * 「1パラメータだけ動かす山登り法」では収束しない。[RnnParams.mutate] は**全パラメータに
 * 独立な小さいガウスノイズを同時に加える**、標準的な (μ, λ)-ES に近い近傍生成を行う。
 *
 * 本番の Playmats/GameServer は既定で [EvalMode.LINEAR]（従来どおり EvalWeights）を使う。
 * この評価器は [evalMode] を [EvalMode.RNN] に切り替えたときだけ使われる、独立した経路。
 */

/** 現在有効な評価方式。既定は従来の線形モデルで、本番挙動に影響を与えない */
enum class EvalMode { LINEAR, RNN, KANN }

/**
 * `@ThreadLocal`: [activeWeights] と同じ理由。Tuner の自己対戦をスレッド並列化する際、
 * 対戦ごとに `evalMode`/`rnnParams` を切り替える処理が複数スレッドから同時に走るため、
 * プロセス共有のままでは他スレッドの対戦と競合する。スレッドごとに独立させることで、
 * 単一スレッドの本番実行(GameServer/Playmats)には影響を与えずに安全に並列化できる。
 */
@ThreadLocal
var evalMode: EvalMode = EvalMode.LINEAR

/** [EvalMode.RNN] のときに使う重み。未設定(null)なら RNN モードでも従来どおり線形評価にフォールバックする */
@ThreadLocal
var rnnParams: RnnParams? = null

/**
 * [EvalMode.KANN] のときに使うネットワーク(cinterop連携したKANNのGRU価値ネットワーク、
 * [KannValueNetwork])。未設定(null)なら KANN モードでも従来どおり線形評価にフォールバックする。
 * [@ThreadLocal] の理由は [rnnParams] と同じ。
 */
@ThreadLocal
var kannNet: KannValueNetwork? = null

/**
 * player1/player2 それぞれ専用の重み(GUIの設定ダイアログで個別指定できる、[applyEvalConfig]参照)。
 * どちらも未設定なら従来どおり[rnnParams]/[kannNet]を両者で共有する。[Tuner.kt]のES自己対戦は
 * これらとは無関係に`rnnParams`を直接差し替えるので、ここが null のままなら干渉しない。
 *
 * KANNは[kannNet]という単一のグラフ(ann)を使い回し、手番ごとに[KannValueNetwork.setWeights]で
 * 中身を差し替える([KannEsTrainer.kt]の`playoutGameKann`と同じ発想)。RNNは[RnnParams]が
 * 素のKotlinオブジェクトなので、そのまま2つ保持するだけでよい。
 */
@ThreadLocal
var rnnParamsP1: RnnParams? = null
@ThreadLocal
var rnnParamsP2: RnnParams? = null
@ThreadLocal
var kannWeightsP1: FloatArray? = null
@ThreadLocal
var kannWeightsP2: FloatArray? = null

/**
 * [state.choosingPlayerId] に応じて、そのプレイヤー専用の重み([rnnParamsP1]/[rnnParamsP2]、
 * [kannWeightsP1]/[kannWeightsP2])が設定されていれば「現在有効な」重み([rnnParams]の中身、
 * または[kannNet]の重み配列)へ反映する。片方(または両方)が未設定ならそのプレイヤーには
 * 何もしない(従来どおり共有の重みのまま)。
 *
 * [enumerateActions] と [stepEndEval] の両方から呼ぶ(選択肢とステップ終了の基準値は同じ評価器
 * でなければ比較が成立しない、[stepEndEval]のコメント参照) — ここに一本化することで、
 * 過去に実際に起きた「片方だけ更新し忘れる」バグを繰り返さない。
 */
fun syncEvalForTurn(state: GameState) {
    if (evalMode == EvalMode.RNN) {
        val p = if (state.choosingPlayerId == 1) rnnParamsP1 else rnnParamsP2
        if (p != null) rnnParams = p
    } else if (evalMode == EvalMode.KANN) {
        val w = if (state.choosingPlayerId == 1) kannWeightsP1 else kannWeightsP2
        val net = kannNet
        if (w != null && net != null) net.setWeights(w)
    }
}

/** 1枚のカードを表すトークンの次元数 */
const val RNN_TOKEN_DIM = 10

/** カード列を集約するGRUの隠れ状態次元数 */
const val RNN_HIDDEN_DIM = 16

/** カード列にできない大域的なスカラー特徴の次元数 (両者のライフ・リザーブ等) */
const val RNN_GLOBAL_DIM = 12

/** 結合後の全結合隠れ層の次元数 */
const val RNN_DENSE_DIM = 16

// ===================== 小さな線形代数ヘルパー (外部ライブラリなし) =====================

private fun sigmoid(x: Double): Double = 1.0 / (1.0 + exp(-x))

/** 行列(H×I) × ベクトル(I) = ベクトル(H) */
private fun matVec(w: Array<DoubleArray>, x: DoubleArray): DoubleArray {
    val h = w.size
    val out = DoubleArray(h)
    for (i in 0 until h) {
        var s = 0.0
        val row = w[i]
        for (j in x.indices) s += row[j] * x[j]
        out[i] = s
    }
    return out
}

private fun addInPlace(a: DoubleArray, b: DoubleArray): DoubleArray {
    for (i in a.indices) a[i] += b[i]
    return a
}

private fun elementwiseMul(a: DoubleArray, b: DoubleArray): DoubleArray =
    DoubleArray(a.size) { a[it] * b[it] }

private fun mapSigmoid(a: DoubleArray): DoubleArray = DoubleArray(a.size) { sigmoid(a[it]) }
private fun mapTanh(a: DoubleArray): DoubleArray = DoubleArray(a.size) { tanh(a[it]) }

private fun randomMatrix(rows: Int, cols: Int, rng: Random, scale: Double): Array<DoubleArray> =
    Array(rows) { DoubleArray(cols) { (rng.nextDouble() * 2.0 - 1.0) * scale } }

private fun randomVector(size: Int, rng: Random, scale: Double): DoubleArray =
    DoubleArray(size) { (rng.nextDouble() * 2.0 - 1.0) * scale }

// ===================== GRUの重み =====================

/**
 * GRU 1層 + 全結合2層のすべての重みをまとめたもの。
 * ES で扱えるよう、[flatten]/[withFlat] でフラットな `DoubleArray` と相互変換できる。
 */
class RnnParams(
    val wz: Array<DoubleArray>, val uz: Array<DoubleArray>, val bz: DoubleArray,
    val wr: Array<DoubleArray>, val ur: Array<DoubleArray>, val br: DoubleArray,
    val wh: Array<DoubleArray>, val uh: Array<DoubleArray>, val bh: DoubleArray,
    val denseW: Array<DoubleArray>, val denseB: DoubleArray,
    val outW: DoubleArray, val outB: DoubleArray
) {
    companion object {
        /** 全パラメータ数 (GRU 3ゲート + Dense隠れ層 + 出力層) */
        val paramCount: Int = run {
            val gru = 3 * (RNN_HIDDEN_DIM * RNN_TOKEN_DIM + RNN_HIDDEN_DIM * RNN_HIDDEN_DIM + RNN_HIDDEN_DIM)
            val dense = (RNN_HIDDEN_DIM + RNN_GLOBAL_DIM) * RNN_DENSE_DIM + RNN_DENSE_DIM
            val out = RNN_DENSE_DIM * 1 + 1
            gru + dense + out
        }

        /** tanh/sigmoid が序盤から飽和しないよう、小さい一様乱数で初期化する */
        fun random(rng: Random, scale: Double = 0.3): RnnParams = RnnParams(
            wz = randomMatrix(RNN_HIDDEN_DIM, RNN_TOKEN_DIM, rng, scale),
            uz = randomMatrix(RNN_HIDDEN_DIM, RNN_HIDDEN_DIM, rng, scale),
            bz = randomVector(RNN_HIDDEN_DIM, rng, scale),
            wr = randomMatrix(RNN_HIDDEN_DIM, RNN_TOKEN_DIM, rng, scale),
            ur = randomMatrix(RNN_HIDDEN_DIM, RNN_HIDDEN_DIM, rng, scale),
            br = randomVector(RNN_HIDDEN_DIM, rng, scale),
            wh = randomMatrix(RNN_HIDDEN_DIM, RNN_TOKEN_DIM, rng, scale),
            uh = randomMatrix(RNN_HIDDEN_DIM, RNN_HIDDEN_DIM, rng, scale),
            bh = randomVector(RNN_HIDDEN_DIM, rng, scale),
            denseW = randomMatrix(RNN_DENSE_DIM, RNN_HIDDEN_DIM + RNN_GLOBAL_DIM, rng, scale),
            denseB = randomVector(RNN_DENSE_DIM, rng, scale),
            outW = randomVector(RNN_DENSE_DIM, rng, scale),
            outB = randomVector(1, rng, scale)
        )
    }

    /** すべての重みを1本の `DoubleArray` に平坦化する (ES の近傍生成・保存用) */
    fun flatten(): DoubleArray {
        val out = ArrayList<Double>(paramCount)
        fun addM(m: Array<DoubleArray>) { for (row in m) for (v in row) out.add(v) }
        fun addV(v: DoubleArray) { for (x in v) out.add(x) }
        addM(wz); addM(uz); addV(bz)
        addM(wr); addM(ur); addV(br)
        addM(wh); addM(uh); addV(bh)
        addM(denseW); addV(denseB)
        addV(outW); addV(outB)
        return out.toDoubleArray()
    }

    /** [flatten] の逆。同じ並び順で読み戻して新しい [RnnParams] を作る */
    fun withFlat(flat: DoubleArray): RnnParams {
        var i = 0
        fun nextM(rows: Int, cols: Int): Array<DoubleArray> =
            Array(rows) { DoubleArray(cols) { flat[i++] } }
        fun nextV(size: Int): DoubleArray = DoubleArray(size) { flat[i++] }

        val newWz = nextM(RNN_HIDDEN_DIM, RNN_TOKEN_DIM)
        val newUz = nextM(RNN_HIDDEN_DIM, RNN_HIDDEN_DIM)
        val newBz = nextV(RNN_HIDDEN_DIM)
        val newWr = nextM(RNN_HIDDEN_DIM, RNN_TOKEN_DIM)
        val newUr = nextM(RNN_HIDDEN_DIM, RNN_HIDDEN_DIM)
        val newBr = nextV(RNN_HIDDEN_DIM)
        val newWh = nextM(RNN_HIDDEN_DIM, RNN_TOKEN_DIM)
        val newUh = nextM(RNN_HIDDEN_DIM, RNN_HIDDEN_DIM)
        val newBh = nextV(RNN_HIDDEN_DIM)
        val newDenseW = nextM(RNN_DENSE_DIM, RNN_HIDDEN_DIM + RNN_GLOBAL_DIM)
        val newDenseB = nextV(RNN_DENSE_DIM)
        val newOutW = nextV(RNN_DENSE_DIM)
        val newOutB = nextV(1)
        return RnnParams(newWz, newUz, newBz, newWr, newUr, newBr, newWh, newUh, newBh, newDenseW, newDenseB, newOutW, newOutB)
    }

    /**
     * 全パラメータに独立な小さいガウスノイズを同時に加えた近傍解を返す (単純なES/OpenAI-ES型の摂動)。
     * `EvalWeights.mutate` の「1個だけ動かす」山登り法は、次元数が1780もあると事実上収束しないため、
     * ここでは全次元を少しずつ動かす方式にしている。
     */
    fun mutate(rng: Random, sigma: Double = 0.05): RnnParams {
        val flat = flatten()
        for (i in flat.indices) {
            // Box-Muller法で標準正規乱数を作る
            val u1 = rng.nextDouble().coerceAtLeast(1e-12)
            val u2 = rng.nextDouble()
            val gaussian = kotlin.math.sqrt(-2.0 * kotlin.math.ln(u1)) * kotlin.math.cos(2.0 * kotlin.math.PI * u2)
            flat[i] += gaussian * sigma
        }
        return withFlat(flat)
    }
}

/**
 * 学習済みRNN重みの既定の保存先。GameServer/Playmats いずれのカレントディレクトリからも
 * 同じ相対パスで見つかるよう、プロジェクトルート直下に置く運用を想定する。
 * Protocol Buffers形式のバイナリなので拡張子は `.pb`。
 */
const val RNN_WEIGHTS_DEFAULT_PATH = "rnn-weights.pb"

/** [RnnParams.flatten] の結果をProtocol Buffersでシリアライズするためのラッパー */
@Serializable
private data class RnnWeightsProto(val flat: List<Double>)

/**
 * `RnnParams.flatten()` の結果をProtocol Buffers形式で保存する。
 * これまで `tuneRnnWeights` の学習結果は変数として作られるだけで永続化されず、
 * チューニングを実行しても何も更新されない状態だった。
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalSerializationApi::class)
fun saveRnnParams(params: RnnParams, path: String = RNN_WEIGHTS_DEFAULT_PATH): Boolean {
    val f = fopen(path, "wb") ?: return false
    val bytes = ProtoBuf.encodeToByteArray(RnnWeightsProto(params.flatten().toList()))
    bytes.usePinned { pinned -> fwrite(pinned.addressOf(0), 1u, bytes.size.convert(), f) }
    fclose(f)
    return true
}

/**
 * [saveRnnParams] で保存した重みを読み込む。ファイルが無い・破損している・パラメータ数が
 * 現行の次元定義 ([RnnParams.paramCount]) と食い違う場合は null を返し、呼び出し側が
 * 乱数初期化にフォールバックできるようにする (次元定数を変更した後に古い保存ファイルを
 * 誤って読み込まないための安全策)。
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalSerializationApi::class)
fun loadRnnParams(path: String = RNN_WEIGHTS_DEFAULT_PATH): RnnParams? {
    val f = fopen(path, "rb") ?: return null
    fseek(f, 0, SEEK_END)
    val size = ftell(f)
    fseek(f, 0, SEEK_SET)
    if (size <= 0) {
        fclose(f)
        return null
    }
    val sizeInt = size.toInt()
    val bytes = memScoped {
        val buf = allocArray<ByteVar>(sizeInt)
        fread(buf, 1u, size.convert(), f)
        ByteArray(sizeInt) { buf[it] }
    }
    fclose(f)
    val flat = try {
        ProtoBuf.decodeFromByteArray<RnnWeightsProto>(bytes).flat.toDoubleArray()
    } catch (e: Exception) {
        return null
    }
    if (flat.size != RnnParams.paramCount) return null
    // withFlat は受け手の中身に依存せず flat の値だけで全パラメータを再構築するため、
    // ここでの random() はサイズ合わせの空箱に過ぎない (scale=0.0 で無駄な計算も避ける)
    return RnnParams.random(Random(0), scale = 0.0).withFlat(flat)
}

/** GRUを1ステップ進める。h は上書きせず新しい隠れ状態を返す */
private fun gruStep(p: RnnParams, x: DoubleArray, h: DoubleArray): DoubleArray {
    val z = mapSigmoid(addInPlace(addInPlace(matVec(p.wz, x), matVec(p.uz, h)), p.bz))
    val r = mapSigmoid(addInPlace(addInPlace(matVec(p.wr, x), matVec(p.ur, h)), p.br))
    val rh = elementwiseMul(r, h)
    val hCandidate = mapTanh(addInPlace(addInPlace(matVec(p.wh, x), matVec(p.uh, rh)), p.bh))
    return DoubleArray(h.size) { i -> (1.0 - z[i]) * h[i] + z[i] * hCandidate[i] }
}

// ===================== 局面 → トークン列・大域特徴への変換 =====================

private fun categoryFeature(c: CardCategory): Double = when (c) {
    CardCategory.SPIRIT -> 0.0
    CardCategory.NEXUS -> 0.33
    CardCategory.MAGIC -> 0.66
    else -> 1.0
}

/** 手札のカード1枚分のトークン。まだ場に出ていないので BP/コア/疲労は0固定 */
private fun tokenFromHandCard(card: CardData): DoubleArray = doubleArrayOf(
    card.cost / 10.0,
    0.0, // BP (手札にはまだ無い)
    0.0, // Lv (手札にはまだ無い)
    0.0, // コア数
    0.0, // ソウルコアの有無
    0.0, // 疲労状態
    card.symbols.size / 3.0,
    0.0, // zone = 自分の手札
    categoryFeature(card.category),
    card.reductionSymbols.size / 3.0
)

/** フィールドのカード1枚分のトークン。zone で自分の場(0.25)か相手の場(0.5)かを区別する */
private fun tokenFromFieldCard(fc: FieldCard, zone: Double): DoubleArray = doubleArrayOf(
    0.0, // コストは支払い済みなので参照しない
    fc.currentBp / 10000.0,
    fc.level / 3.0,
    fc.cores.total / 10.0,
    if (fc.cores.soul > 0) 1.0 else 0.0,
    if (fc.isExhausted) 1.0 else 0.0,
    fc.symbols.size / 3.0,
    zone,
    categoryFeature(fc.category),
    0.0 // 軽減シンボルは場では参照しない
)

/**
 * トラッシュのカード1枚分のトークン。トラッシュは公開情報 (総合ルール 4-4-2) なので
 * 自分・相手どちらの中身も参照してよい。すでに場を離れているので、フィールドのカードと違い
 * BP/コア/疲労は持たず、手札のカードと同じ「静的なカード情報のみ」のトークンになる
 * (`tokenFromHandCard` と同じ形。zone だけで手札・自陣トラッシュ・敵陣トラッシュを区別する)。
 */
private fun tokenFromTrashCard(card: CardData, zone: Double): DoubleArray = doubleArrayOf(
    card.cost / 10.0,
    0.0, // BP (トラッシュにいるので参照不可)
    0.0, // Lv (トラッシュにいるので参照不可)
    0.0, // コア数 (破壊時にリザーブへ戻るのでトラッシュのカードは持たない)
    0.0, // ソウルコアの有無
    0.0, // 疲労状態
    card.symbols.size / 3.0,
    zone,
    categoryFeature(card.category),
    card.reductionSymbols.size / 3.0
)

/**
 * 「手札(自分) → 自分の場 → 相手の場 → 自分のトラッシュ → 相手のトラッシュ」の順でトークン列を作る。
 *
 * 手札・場・トラッシュは**同じ「カードがある場所」でもゲーム上まったく別の意味を持つ**ため、
 * zone の値を全ゾーンで重複させず区別する:
 *   手札(自分)=0.0 / 場(自分)=0.25 / 場(相手)=0.5 / トラッシュ(自分)=0.75 / トラッシュ(相手)=1.0
 * 手札のカードは「まだ場に出ていないスピリットカード」、場のカードは「召喚された結果生成された
 * スピリット/ネクサスのインスタンス」、トラッシュのカードは「破壊・使用済みで場を離れたカード」であり、
 * これらを同じ特徴として混同すると評価器が区別できなくなる。
 *
 * 相手の手札は非公開情報なので含めない (総合ルール 4-3-2/4-3-3、GodMode/PlayModeの区分と同じ原則)。
 * トラッシュは自分・相手とも公開情報 (4-4-2) なので両方含める。
 * デッキは所有者自身にも非公開 (4-2-2) なのでどちらのデッキも枚数(大域特徴)のみで中身は含めない。
 */
fun buildTokenSequence(p: PlayerState, opp: PlayerState): List<DoubleArray> {
    val tokens = ArrayList<DoubleArray>(p.hand.size + p.field.size + opp.field.size + p.trashCards.size + opp.trashCards.size)
    for (c in p.hand) tokens.add(tokenFromHandCard(c))
    for (fc in p.field) tokens.add(tokenFromFieldCard(fc, zone = 0.25))
    for (fc in opp.field) tokens.add(tokenFromFieldCard(fc, zone = 0.5))
    for (c in p.trashCards) tokens.add(tokenFromTrashCard(c, zone = 0.75))
    for (c in opp.trashCards) tokens.add(tokenFromTrashCard(c, zone = 1.0))
    return tokens
}

/** 系列にできない大域的なスカラー特徴。すべて公開情報 + 自分の非公開情報のみ (相手手札の中身は使わない) */
fun buildGlobalFeatures(state: GameState, p: PlayerState, opp: PlayerState): DoubleArray = doubleArrayOf(
    p.life / 10.0,
    opp.life / 10.0,
    p.reserve.total / 10.0,
    opp.reserve.total / 10.0,
    p.trash.total / 10.0,
    opp.trash.total / 10.0,
    p.hand.size / 10.0,
    opp.hand.size / 10.0, // 公開情報(枚数)のみ。中身は使わない
    p.deckCount / 40.0,
    opp.deckCount / 40.0,
    state.turn / 20.0,
    1.0 // バイアス項
)

/**
 * 局面価値 V(state) を `forPlayerId` 視点で評価する。戻り値は tanh の出力なので [-1, 1]。
 * 既存の `eval` フィールドと同じスケールで扱えるようにしている。
 */
fun evaluateStateWithRnn(params: RnnParams, state: GameState, forPlayerId: Int): Double {
    val p = if (forPlayerId == 1) state.player1 else state.player2
    val opp = if (forPlayerId == 1) state.player2 else state.player1

    var h = DoubleArray(RNN_HIDDEN_DIM)
    for (token in buildTokenSequence(p, opp)) {
        h = gruStep(params, token, h)
    }

    val globals = buildGlobalFeatures(state, p, opp)
    val concat = h + globals // DoubleArray の '+' は連結 (要素同士の加算ではない)
    val hidden = mapTanh(addInPlace(matVec(params.denseW, concat), params.denseB))

    var out = params.outB[0]
    for (i in hidden.indices) out += params.outW[i] * hidden[i]
    return tanh(out)
}

/**
 * 選択肢1件の評価値を、「その手を仮に適用した後の局面」への V(state) として求める(1手先読み)。
 * 召喚・コア移動・アタック・ブロックのすべてを同じ評価器で扱えるのが線形モデルとの違い。
 */
fun evaluateActionWithRnn(state: GameState, action: GameAction, params: RnnParams): Double {
    val trial = state.snapshot()
    val messages = mutableListOf<String>()
    if (action.type == "END_STEP") {
        advanceStep(trial, messages)
    } else {
        applyAction(trial, action, messages)
    }
    return evaluateStateWithRnn(params, trial, state.choosingPlayerId)
}

/**
 * 「ステップ終了」自体の評価値。選択肢の eval と比較する唯一の基準値なので、
 * 選択肢と同じ評価器を使わなければ比較が成立しない。
 *
 * これは Playmats(WebUI表示) と Tuner(自己対戦シミュレーション/デバッグ)の両方から呼ばれる
 * **唯一の実装**であること。過去に Playmats 側だけがこの計算を独自に持っていたため、
 * RNN評価器を導入したときに Playmats の方だけ更新し忘れ、
 * 「選択肢はRNNで計算されるのにステップ終了の基準値だけ線形モデルの定数(0.45)のまま」
 * という比較不能なバグを実際に起こした。二度と複製しないこと。
 */
fun stepEndEval(state: GameState): Double {
    syncEvalForTurn(state)
    val params = rnnParams
    if (evalMode == EvalMode.RNN && params != null) {
        val endStep = GameAction(index = 999, type = "END_STEP", category = "", detail = "", eval = 0.0)
        return evaluateActionWithRnn(state, endStep, params)
    }
    val kann = kannNet
    if (evalMode == EvalMode.KANN && kann != null) {
        return kannStepEndEval(state, kann)
    }
    return activeWeights.stepEndEval
}

/**
 * 評価方式(`evalMode`、および player1/player2 それぞれの重み)を切り替える。
 * GameServer/Playmats それぞれの起動時CLIフラグ(`--eval-rnn`/`--eval-kann`)と、実行中に
 * 切り替える `/api/eval-config` エンドポイント(設定ダイアログの「評価方式を適用」)の
 * 両方から使う共通実装(重複させない)。
 *
 * `weightsPathP1`/`weightsPathP2` はそれぞれ独立に省略できる。片方だけ指定した場合、
 * 指定しなかった側の手番では[syncEvalForTurn]が何もしないため、直前に有効だった重み
 * (通常はもう一方のプレイヤーの重み)がそのまま使われる — 実質「未指定側はplayer1/2で共有」
 * になる。両方省略した場合、RNNは乱数初期化(`RnnParams.random`)、KANNは未学習の新規グラフ
 * (`KannValueNetwork()`)にフォールバックする(ただし既に読み込み済みなら上書きしない)。
 *
 * @return "ok" なら成功、それ以外はユーザーに見せてよいエラーメッセージ
 */
fun applyEvalConfig(mode: String, weightsPathP1: String?, weightsPathP2: String?): String {
    when (mode.uppercase()) {
        "LINEAR" -> evalMode = EvalMode.LINEAR
        "RNN" -> {
            evalMode = EvalMode.RNN
            if (!weightsPathP1.isNullOrBlank()) {
                rnnParamsP1 = loadRnnParams(weightsPathP1)
                    ?: return "player1の重みファイルを読み込めません: $weightsPathP1"
            }
            if (!weightsPathP2.isNullOrBlank()) {
                rnnParamsP2 = loadRnnParams(weightsPathP2)
                    ?: return "player2の重みファイルを読み込めません: $weightsPathP2"
            }
            if (rnnParams == null && rnnParamsP1 == null && rnnParamsP2 == null) {
                rnnParams = RnnParams.random(Random(42))
            }
        }
        "KANN" -> {
            evalMode = EvalMode.KANN
            if (kannNet == null) kannNet = KannValueNetwork()
            val net = kannNet!!
            if (!weightsPathP1.isNullOrBlank()) {
                val loaded = KannValueNetwork.load(weightsPathP1)
                    ?: return "player1の重みファイルを読み込めません: $weightsPathP1"
                val w = loaded.getWeights()
                loaded.delete()
                if (w.size != net.nVar) return "player1の重みファイルのパラメータ数(${w.size})がネットワーク構成(${net.nVar})と一致しません"
                kannWeightsP1 = w
            }
            if (!weightsPathP2.isNullOrBlank()) {
                val loaded = KannValueNetwork.load(weightsPathP2)
                    ?: return "player2の重みファイルを読み込めません: $weightsPathP2"
                val w = loaded.getWeights()
                loaded.delete()
                if (w.size != net.nVar) return "player2の重みファイルのパラメータ数(${w.size})がネットワーク構成(${net.nVar})と一致しません"
                kannWeightsP2 = w
            }
        }
        else -> return "不明な評価方式: $mode"
    }
    return "ok"
}
