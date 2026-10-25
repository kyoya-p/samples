package bstools.model

import bstools.kann.*
import kotlinx.cinterop.*

/**
 * KANN (cinterop 経由でリンクした C言語のNNライブラリ) を使った局面価値関数 V(state) の実装。
 *
 * [RnnEvaluator.kt] の自作GRU+ES学習とは独立した経路。こちらは KANN の計算グラフ
 * (reverse-mode自動微分) を使うため、将来的にESではなく本物のBPTT(Backpropagation
 * Through Time)による学習に置き換えられる。学習ループ自体は未実装で、現時点では
 * ネットワーク構築と1手先読み用のフォワード評価のみを提供する。
 *
 * 特徴量の作り方は [RnnEvaluator.kt] の GRU 版と共通化する ([buildTokenSequence]/
 * [buildGlobalFeatures] を再利用) — 学習方式が変わっても「何を見て評価するか」は
 * 変えない。
 *
 * グラフ構成:
 *   トークン入力(RNN_TOKEN_DIM, 手番ごとに供給) → GRU(RNN_HIDDEN_DIM)
 *   大域特徴入力(RNN_GLOBAL_DIM, 毎ステップ同じ値を供給) → [GRU出力と結合]
 *   → Dense(RNN_DENSE_DIM) → tanh → kann_layer_cost が内部で Dense(1)+tanh を追加し、
 *     KANN_C_CEB_NEG (tanhベースの二値交差エントロピー) で学習可能な形にする。
 *   出力は tanh の値域 [-1, 1] で、既存の eval 値と同じスケールに揃えている。
 *
 * 大域特徴は本来1回だけ与えれば十分だが、KANN の連続フィード方式
 * (`kann_rnn_start`/`kann_apply1`相当)はトークンを1個処理するたびにグラフ全体を
 * 評価するため、実装を単純にするためトークンと同じタイミングで(同じ値を)毎回供給する。
 */
data class KannArchConfig(
    val tokenDim: Int = RNN_TOKEN_DIM,
    val hiddenDim: Int = RNN_HIDDEN_DIM,
    val globalDim: Int = RNN_GLOBAL_DIM,
    val denseDim: Int = RNN_DENSE_DIM,
    val numDenseLayers: Int = 1
)

@OptIn(ExperimentalForeignApi::class)
class KannValueNetwork private constructor(
    private val ann: CPointer<kann_t>,
    val config: KannArchConfig = KannArchConfig()
) {
    private val tokenLabel = 0
    private val globalLabel = 1

    /** 学習対象の変数(重み)の総数。RMSpropの更新対象サイズと、永続メモリ配列のサイズに使う */
    val nVar: Int = kad_size_var(ann.pointed.n, ann.pointed.v)

    /**
     * RMSpropの内部メモリ(パラメータごとの二乗勾配の移動平均)。学習ステップをまたいで
     * 保持する必要があるため、`memScoped` ではなく `nativeHeap` で確保し `delete()` で解放する。
     */
    private val rmsMemory: CPointer<FloatVar> = nativeHeap.allocArray(nVar) { value = 0f }

    /** 未学習のランダム初期化で新規グラフを構築するコンストラクタ */
    constructor(config: KannArchConfig = KannArchConfig()) : this(buildGraph(config), config)

    /**
     * `kann_unroll_array` で展開した学習用ネットワーク一式(展開後のグラフ本体＋そこに
     * bind する永続入力バッファ)を展開長ごとに束ねたもの。
     */
    private class UnrolledBundle(
        val ua: CPointer<kann_t>,
        val iLastCost: Int,
        val tokBufs: Array<CPointer<FloatVar>>,
        val globBuf: CPointer<FloatVar>,
        val truthBuf: CPointer<FloatVar>,
        val xArr: CPointer<CPointerVar<FloatVar>>,
        val gArr: CPointer<CPointerVar<FloatVar>>,
        val yArr: CPointer<CPointerVar<FloatVar>>
    ) {
        fun freeBuffers() {
            for (buf in tokBufs) nativeHeap.free(buf)
            nativeHeap.free(globBuf)
            nativeHeap.free(truthBuf)
            nativeHeap.free(xArr)
            nativeHeap.free(gArr)
            nativeHeap.free(yArr)
        }
    }

    private val unrolledCache = mutableMapOf<Int, UnrolledBundle>()

    // evaluate() 用の永続バッファ (memScoped のダングリングポインタ防止・高速化)
    private val evalTokBuf: CPointer<FloatVar> = nativeHeap.allocArray(RNN_TOKEN_DIM) { value = 0f }
    private val evalGlobBuf: CPointer<FloatVar> = nativeHeap.allocArray(RNN_GLOBAL_DIM) { value = 0f }
    private val evalTokPtrArr: CPointer<CPointerVar<FloatVar>> = nativeHeap.allocArray<CPointerVar<FloatVar>>(1).also {
        it[0] = evalTokBuf
    }
    private val evalGlobPtrArr: CPointer<CPointerVar<FloatVar>> = nativeHeap.allocArray<CPointerVar<FloatVar>>(1).also {
        it[0] = evalGlobBuf
    }

    private fun unrolledFor(len: Int): UnrolledBundle = unrolledCache.getOrPut(len) {
        val ua = memScoped {
            val lenArr = allocArray<IntVar>(1)
            lenArr[0] = len
            val u = kann_unroll_array(ann, lenArr) ?: error("kann_unroll_array failed (len=$len)")
            kad_sync_dim(u.pointed.n, u.pointed.v, 1)
            u
        }
        // ピボットノード(ua->v[ua->n - 1], kad_avg)の最後の子が最終時刻(t = len - 1)のコストノード。
        // evaluate() は全トークンを読み終えた最終時刻の出力のみを局面評価値 V(s) として使うため、
        // 学習時も途中時刻の平均コストではなく最終時刻のコストノードのみから逆伝播する。
        val nNodes = ua.pointed.n
        val vNodes = ua.pointed.v!!
        val pivot = vNodes[nNodes - 1]!!
        val lastChildPtr = pivot.pointed.child!![len - 1]
        var iLastCost = nNodes - 2
        for (idx in nNodes - 1 downTo 0) {
            if (vNodes[idx] == lastChildPtr) {
                iLastCost = idx
                break
            }
        }

        val tokBufs = Array(len) { nativeHeap.allocArray<FloatVar>(RNN_TOKEN_DIM) { value = 0f } }
        val globBuf = nativeHeap.allocArray<FloatVar>(RNN_GLOBAL_DIM) { value = 0f }
        val truthBuf = nativeHeap.allocArray<FloatVar>(1).also { it[0] = 0f }
        val xArr = nativeHeap.allocArray<CPointerVar<FloatVar>>(len)
        val gArr = nativeHeap.allocArray<CPointerVar<FloatVar>>(len)
        val yArr = nativeHeap.allocArray<CPointerVar<FloatVar>>(len)
        for (i in 0 until len) {
            xArr[i] = tokBufs[i]
            gArr[i] = globBuf
            yArr[i] = truthBuf
        }
        bindFeedExact(ua, KANN_F_IN.convert(), tokenLabel, xArr)
        bindFeedExact(ua, KANN_F_IN.convert(), globalLabel, gArr)
        bindFeedExact(ua, KANN_F_TRUTH.convert(), 0, yArr)
        UnrolledBundle(ua, iLastCost, tokBufs, globBuf, truthBuf, xArr, gArr, yArr)
    }

    /**
     * 重み配列(`ann->x`、サイズ[nVar])のコピーを取得する。
     */
    fun getWeights(): FloatArray = FloatArray(nVar) { ann.pointed.x!![it] }

    /** [getWeights] で取得した配列を書き戻す。サイズが[nVar]と一致しない場合は無視する */
    fun setWeights(w: FloatArray) {
        if (w.size != nVar) return
        for (i in 0 until nVar) ann.pointed.x!![i] = w[i]
    }

    /**
     * KANNの `kann_feed_bind` は `ext_label == 0` を「全ラベル一致(ワイルドカード)」として扱う仕様
     * (`#define chk_lbl(label, query) ((query) == 0 || (label) == (query))`) があるため、
     * `tokenLabel = 0` を渡すと `globIn (ext_label = 1)` にまでマッチして配列外参照(クラッシュ)を引き起こす。
     * これを防ぐため、`ext_label` の厳密一致でフィードノードにバインドする。
     */
    private fun bindFeedExact(
        a: CPointer<kann_t>,
        extFlag: UInt,
        extLabel: Int,
        x: CPointer<CPointerVar<FloatVar>>
    ): Int {
        var k = 0
        val n = a.pointed.n
        val v = a.pointed.v ?: return 0
        for (i in 0 until n) {
            val node = v[i] ?: continue
            val isFeed = node.pointed.n_child == 0 && (node.pointed.flag.toInt() and (KAD_VAR or KAD_CONST)) == 0
            if (isFeed && (node.pointed.ext_flag and extFlag) != 0u && node.pointed.ext_label == extLabel) {
                node.pointed.x = x[k++]
            }
        }
        return k
    }

    /**
     * トークン列(可変長)と大域特徴を与えて V(state) を1回のRNN連続フィードで評価する。
     */
    fun evaluate(tokens: List<DoubleArray>, globalFeatures: DoubleArray): Double {
        if (tokens.isEmpty()) return 0.0
        kann_rnn_start(ann)
        var result = 0.0
        for (j in globalFeatures.indices) evalGlobBuf[j] = globalFeatures[j].toFloat()

        val iOut = kann_find(ann, KANN_F_OUT.convert(), 0)
        if (iOut < 0) error("kann_find(KANN_F_OUT) failed: $iOut")

        kad_sync_dim(ann.pointed.n, ann.pointed.v, 1)
        bindFeedExact(ann, KANN_F_IN.convert(), tokenLabel, evalTokPtrArr)
        bindFeedExact(ann, KANN_F_IN.convert(), globalLabel, evalGlobPtrArr)

        for (tok in tokens) {
            for (j in tok.indices) evalTokBuf[j] = tok[j].toFloat()
            val outPtr = kad_eval_at(ann.pointed.n, ann.pointed.v, iOut)
            result = outPtr?.get(0)?.toDouble() ?: 0.0
        }
        kann_rnn_end(ann)
        return result
    }

    /**
     * トークン列1件分について、真の勝敗ラベル(勝=1.0 / 負=0.0 / 引分=0.5、後で[-1,1]へ再変換)で
     * 1ステップ学習する (BPTT: `kann_unroll_array` で時刻数ぶん展開→最終時刻コストノードでforward+backward
     * →`kann_RMSprop`で重み更新)。展開後のネットワークは重み配列(x/g)を元の[ann]と共有するため、
     * ここでの更新がそのまま[ann]（延いては[evaluate]）に反映される。
     *
     * @return このステップのコスト(小さいほど正解に近い)
     */
    fun trainStep(tokens: List<DoubleArray>, globals: DoubleArray, label: Double, lr: Float): Float {
        if (tokens.isEmpty()) return 0f
        val effectiveTokens = if (tokens.size > MAX_TRAIN_UNROLL_LEN) tokens.takeLast(MAX_TRAIN_UNROLL_LEN) else tokens
        val len = effectiveTokens.size
        // ラベル(勝=1.0/負=0.0/引分=0.5)を出力層の tanh 値域[-1,1]に合わせて再スケールする
        val truthValue = (label * 2.0 - 1.0).toFloat()
        val bundle = unrolledFor(len)

        for (i in 0 until len) {
            val src = effectiveTokens[i]
            val dst = bundle.tokBufs[i]
            for (j in 0 until RNN_TOKEN_DIM) dst[j] = src[j].toFloat()
        }
        for (j in globals.indices) bundle.globBuf[j] = globals[j].toFloat()
        bundle.truthBuf[0] = truthValue

        kann_switch(bundle.ua, 1)
        val costPtr = kad_eval_at(bundle.ua.pointed.n, bundle.ua.pointed.v, bundle.iLastCost)
        val cost = costPtr?.get(0) ?: 0f
        kad_grad(bundle.ua.pointed.n, bundle.ua.pointed.v, bundle.iLastCost)
        kann_RMSprop(nVar, lr, null, 0.9f, ann.pointed.g, ann.pointed.x, rmsMemory)
        return cost
    }

    /** 別の候補重みを学習する前にRMSpropの移動平均バッファをゼロクリアする */
    fun resetRmsMemory() {
        for (i in 0 until nVar) rmsMemory[i] = 0f
    }

    /**
     * 複数スレッドでの並列自己対戦・並列評価用に、このネットワークと同一構造かつ独立した
     * C側計算グラフ(`kann_t*`)・重みバッファを持つクローンを生成する。
     */
    fun cloneNet(): KannValueNetwork {
        val cloned = kann_clone(ann, 0) ?: error("kann_clone failed")
        return KannValueNetwork(cloned, config)
    }

    /** KANN組み込みのモデルI/O(`kann_save`)で学習済み重みをバイナリファイルに保存する */
    fun save(path: String) {
        val targetPath = if (!path.contains('/') && !path.contains('\\')) "$WEIGHTS_DIR/$path" else path
        kann_save(targetPath, ann)
    }

    fun delete() {
        for (bundle in unrolledCache.values) {
            bundle.freeBuffers()
            kann_delete_unrolled(bundle.ua)
        }
        unrolledCache.clear()
        nativeHeap.free(evalTokBuf)
        nativeHeap.free(evalGlobBuf)
        nativeHeap.free(evalTokPtrArr)
        nativeHeap.free(evalGlobPtrArr)
        nativeHeap.free(rmsMemory)
        kann_delete(ann)
    }

    companion object {
        /**
         * [trainStep] で `kann_unroll_array` に渡す展開長の上限。
         * `bindFeedExact` により `ext_label=0` のワイルドカード問題が解消されたため、
         * 1局面の全カードトークン列(手札+自陣+敵陣)をそのまま入力できる十分な長さを確保する。
         */
        const val MAX_TRAIN_UNROLL_LEN = 64

        /**
         * [save] で保存したファイルを読み込む。グラフ構造ごとKANN組み込みの形式で
         * 保存されているため、[KannValueNetwork] を新規構築する代わりにこれを使う。
         */
        fun load(path: String): KannValueNetwork? {
            val targetPath = resolveWeightPath(path)
            val loaded = kann_load(targetPath) ?: return null
            return KannValueNetwork(loaded)
        }

        /**
         * 未学習のグラフを構築する。
         * トークン入力(config.tokenDim, 手番ごとに供給) → GRU(config.hiddenDim)
         * 大域特徴入力(config.globalDim, 毎ステップ同じ値を供給) → [GRU出力と結合]
         * → Dense(config.denseDim) × numDenseLayers → tanh → kann_layer_cost が内部で Dense(1)+tanh を追加し、
         *   KANN_C_CEB_NEG (tanhベースの二値交差エントロピー) で学習可能な形にする。
         */
        private fun buildGraph(config: KannArchConfig = KannArchConfig()): CPointer<kann_t> {
            val tokIn = kann_layer_input(config.tokenDim)
                ?: error("kann_layer_input(token) failed")
            val h = kann_layer_gru(tokIn, config.hiddenDim, KANN_RNN_VAR_H0)
                ?: error("kann_layer_gru failed")
            val globIn = kann_layer_input(config.globalDim)
                ?: error("kann_layer_input(global) failed")
            globIn.pointed.ext_label = 1

            var current = memScoped {
                val parts = allocArray<CPointerVar<kad_node_t>>(2)
                parts[0] = h
                parts[1] = globIn
                kad_concat_array(1, 2, parts)
            } ?: error("kad_concat_array failed")

            val layers = maxOf(1, config.numDenseLayers)
            for (l in 0 until layers) {
                val dense = kann_layer_dense(current, config.denseDim)
                    ?: error("kann_layer_dense failed (layer $l)")
                current = kad_tanh(dense) ?: error("kad_tanh failed (layer $l)")
            }

            val costNode = kann_layer_cost(current, 1, KANN_C_CEB_NEG)
                ?: error("kann_layer_cost failed")

            return kann_new(costNode, 0) ?: error("kann_new failed")
        }
    }
}

/**
 * KANNベースの評価器で局面価値 V(state) を評価する。[evaluateStateWithRnn] のKANN版。
 * ネットワークは呼び出しごとに新規作成する(未学習のランダム初期化のため、動作確認用)。
 * 実運用では学習済みネットワークを使い回す必要があり、今後の課題。
 */
fun evaluateStateWithKann(net: KannValueNetwork, state: GameState, forPlayerId: Int): Double {
    val p = if (forPlayerId == 1) state.player1 else state.player2
    val opp = if (forPlayerId == 1) state.player2 else state.player1
    val tokens = buildTokenSequence(p, opp)
    val globals = buildGlobalFeatures(state, p, opp)
    return net.evaluate(tokens, globals)
}

/**
 * 選択肢1件の評価値を、「その手を仮に適用した後の局面」への V(state) として求める(1手先読み)。
 * [evaluateActionWithRnn] のKANN版。[EvalMode.KANN] での [Rules.enumerateActions] 上書きと、
 * KannEsTrainer.kt のES自己対戦の両方から使う共通実装(重複させない、[stepEndEval]のコメント参照)。
 */
fun evaluateActionWithKann(state: GameState, action: GameAction, net: KannValueNetwork): Double {
    state.evaluatedCount++
    val trial = state.snapshot()
    val messages = mutableListOf<String>()
    if (action.type == "END_STEP") advanceStep(trial, messages) else applyAction(trial, action, messages)
    val p = if (state.choosingPlayerId == 1) trial.player1 else trial.player2
    val opp = if (state.choosingPlayerId == 1) trial.player2 else trial.player1
    return net.evaluate(buildTokenSequence(p, opp), buildGlobalFeatures(trial, p, opp))
}

/**
 * 「ステップ終了」自体の評価値を、指定した[net]で評価する。[stepEndEval]はグローバルな
 * [evalMode]/[kannNet]に依存するが、こちらはESの自己対戦([KannEsTrainer.kt])のように
 * 対戦ごとに異なる重みを直接渡したい経路のために、グローバル状態を経由せず引数だけで完結させる。
 */
fun kannStepEndEval(state: GameState, net: KannValueNetwork): Double {
    val endStep = GameAction(index = 999, type = "END_STEP", category = "", detail = "", eval = 0.0)
    return evaluateActionWithKann(state, endStep, net)
}
