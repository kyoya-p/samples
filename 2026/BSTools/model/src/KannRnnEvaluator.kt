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
@OptIn(ExperimentalForeignApi::class)
class KannValueNetwork private constructor(private val ann: CPointer<kann_t>) {
    private val tokenLabel = 0
    private val globalLabel = 1

    /** 学習対象の変数(重み)の総数。RMSpropの更新対象サイズと、永続メモリ配列のサイズに使う */
    val nVar: Int = kad_size_var(ann.pointed.n, ann.pointed.v)

    /**
     * RMSpropの内部メモリ(パラメータごとの二乗勾配の移動平均)。学習ステップをまたいで
     * 保持する必要があるため、`memScoped` ではなく `nativeHeap` で確保し `delete()` で解放する。
     */
    private val rmsMemory: CPointer<FloatVar> = nativeHeap.allocArray(nVar) { value = 0f }

    /** 未学習のランダム初期化で新規グラフを構築する既定コンストラクタ */
    constructor() : this(buildGraph())

    /**
     * `kann_unroll_array` で展開した学習用ネットワーク一式(展開後のグラフ本体＋そこに
     * bind する入力バッファ)を展開長ごとに束ねたもの。バッファも [nativeHeap] で永続化し、
     * 呼び出しごとに使い回す(内容だけ上書きする) — [trainStep] 内で毎回 `memScoped` の
     * 一時バッファを bind すると、そのバッファは関数を抜けると解放されるのに対し、
     * 展開ネットワーク側の feed ノードは同じポインタを次の呼び出しまで保持し続けるため、
     * 呼び出し間でダングリングポインタを指したままになる(実際にこれが原因とみられる
     * クラッシュを確認した)。バッファ自体も展開ネットワークと同じ寿命で永続化することで
     * この期間を無くす。
     */
    private val unrolledCache = mutableMapOf<Int, CPointer<kann_t>>()

    private fun unrolledFor(len: Int): CPointer<kann_t> = unrolledCache.getOrPut(len) {
        memScoped {
            val lenArr = allocArray<IntVar>(1)
            lenArr[0] = len
            val ua = kann_unroll_array(ann, lenArr) ?: error("kann_unroll_array failed (len=$len)")
            kad_sync_dim(ua.pointed.n, ua.pointed.v, 1)
            ua
        }
    }

    /**
     * 重み配列(`ann->x`、サイズ[nVar])のコピーを取得する。ESによる重み最適化
     * ([RnnParams] と同じ発想)で使う。`trainStep`(BPTT)は既知の不具合で不安定なため、
     * こちらはBPTTを一切使わず[evaluate]のみに依存する経路。
     */
    fun getWeights(): FloatArray = FloatArray(nVar) { ann.pointed.x!![it] }

    /** [getWeights] で取得した配列を書き戻す。サイズが[nVar]と一致しない場合は無視する */
    fun setWeights(w: FloatArray) {
        if (w.size != nVar) return
        for (i in 0 until nVar) ann.pointed.x!![i] = w[i]
    }

    /**
     * トークン列(可変長)と大域特徴を与えて V(state) を1回のRNN連続フィードで評価する。
     * [kann_apply1] は単一入力しか扱えないため、2入力(トークン/大域特徴)を自前で
     * bind して都度 [kad_eval_at] で出力ノードだけ評価する、同等の処理を手書きする。
     */
    fun evaluate(tokens: List<DoubleArray>, globalFeatures: DoubleArray): Double {
        if (tokens.isEmpty()) return 0.0
        kann_rnn_start(ann)
        var result = 0.0
        memScoped {
            val tokBuf = allocArray<FloatVar>(RNN_TOKEN_DIM)
            val globBuf = allocArray<FloatVar>(RNN_GLOBAL_DIM)
            for (j in globalFeatures.indices) globBuf[j] = globalFeatures[j].toFloat()

            val tokPtrArr = allocArray<CPointerVar<FloatVar>>(1)
            tokPtrArr[0] = tokBuf
            val globPtrArr = allocArray<CPointerVar<FloatVar>>(1)
            globPtrArr[0] = globBuf

            val iOut = kann_find(ann, KANN_F_OUT.convert(), 0)
            if (iOut < 0) error("kann_find(KANN_F_OUT) failed: $iOut")

            for (tok in tokens) {
                for (j in tok.indices) tokBuf[j] = tok[j].toFloat()
                kad_sync_dim(ann.pointed.n, ann.pointed.v, 1)
                kann_feed_bind(ann, KANN_F_IN.convert(), tokenLabel, tokPtrArr)
                kann_feed_bind(ann, KANN_F_IN.convert(), globalLabel, globPtrArr)
                val outPtr = kad_eval_at(ann.pointed.n, ann.pointed.v, iOut)
                result = outPtr?.get(0)?.toDouble() ?: 0.0
            }
        }
        kann_rnn_end(ann)
        return result
    }

    /**
     * トークン列1件分について、真の勝敗ラベル(勝=1.0 / 負=0.0 / 引分=0.5、後で[-1,1]へ再変換)で
     * 1ステップ学習する (BPTT: `kann_unroll_array` で時刻数ぶん展開→`kann_cost`でforward+backward
     * →`kann_RMSprop`で重み更新)。展開後のネットワークは重み配列(x/g)を元の[ann]と共有するため、
     * ここでの更新がそのまま[ann]（延いては[evaluate]）に反映される。
     *
     * KANNの`kann_layer_gru`は「pivot」として特別扱いされ、展開してもコスト/出力ノードは
     * 単一のまま保たれる(`kann_find`で一意に見つかる) — 公式サンプル `rnn-bit.c` と同じ挙動。
     * 大域特徴は各時刻に同じ値を、正解ラベルも各時刻に同じ値を与える
     * (最終時刻だけを重視したいが、KANNの逐次コスト設計では時刻ごとに与える必要があるため)。
     *
     * **展開ネットワークは削除しない・使い回す** (詳細は[unrolledFor]参照):
     * `kad_delete` は展開後の配列に含まれるノードを無条件に `free()` するが、RNNの重み(葉)
     * ノードは展開前後で「複製」ではなく元の[ann]と共有(同一ポインタ)されているため、
     * 展開ネットワークを削除すると[ann]側の重みノードまで解放されてしまい、次回の学習
     * 呼び出しでクラッシュする(実際に踏んだ)。また、同じ展開長であっても呼び出しのたびに
     * 新規展開するとヒープが壊れ、数十回目の呼び出しで確実にクラッシュすることも実測で
     * 確認した。KANN公式サンプル `rnn-bit.c` の「学習中は一度だけ展開して使い回す」設計
     * 通りに、[unrolledFor] で展開長ごとにキャッシュして再利用する。
     *
     * **`kann_unroll_array` は展開長9以上で不定動作(クラッシュ)することも実測で確認した**
     * (`--kann-train-debug` での二分探索により長さ8までは安定・9以上で確実にクラッシュ)。
     * 原因はKANN側の`kad_unroll_helper`(kautodiff.c)内の実装依存の不具合と見られ、
     * こちら側では修正できないため、学習時のみトークン列を直近[MAX_TRAIN_UNROLL_LEN]件に
     * 切り詰めて回避する。[evaluate]（連続フィード方式、unrollを使わない）はこの制限を
     * 受けず、長さ30程度まで実測で問題なく動作することを確認済み。
     *
     * @return このステップのコスト(小さいほど正解に近い)
     */
    fun trainStep(tokens: List<DoubleArray>, globals: DoubleArray, label: Double, lr: Float): Float {
        if (tokens.isEmpty()) return 0f
        val tokens = if (tokens.size > MAX_TRAIN_UNROLL_LEN) tokens.takeLast(MAX_TRAIN_UNROLL_LEN) else tokens
        val len = tokens.size
        // ラベル(勝=1.0/負=0.0/引分=0.5)を出力層の tanh 値域[-1,1]に合わせて再スケールする
        val truthValue = (label * 2.0 - 1.0).toFloat()
        val ua = unrolledFor(len)

        return memScoped {
            val tokBufs = Array(len) { i ->
                allocArray<FloatVar>(RNN_TOKEN_DIM).also { buf ->
                    for (j in 0 until RNN_TOKEN_DIM) buf[j] = tokens[i][j].toFloat()
                }
            }
            val globBuf = allocArray<FloatVar>(RNN_GLOBAL_DIM)
            for (j in globals.indices) globBuf[j] = globals[j].toFloat()
            val truthBuf = allocArray<FloatVar>(1)
            truthBuf[0] = truthValue

            val xArr = allocArray<CPointerVar<FloatVar>>(len)
            val gArr = allocArray<CPointerVar<FloatVar>>(len)
            val yArr = allocArray<CPointerVar<FloatVar>>(len)
            for (i in 0 until len) {
                xArr[i] = tokBufs[i]
                gArr[i] = globBuf
                yArr[i] = truthBuf
            }

            kann_feed_bind(ua, KANN_F_IN.convert(), tokenLabel, xArr)
            kann_feed_bind(ua, KANN_F_IN.convert(), globalLabel, gArr)
            kann_feed_bind(ua, KANN_F_TRUTH.convert(), 0, yArr)
            kann_switch(ua, 1)
            val cost = kann_cost(ua, 0, 1)
            kann_RMSprop(nVar, lr, null, 0.9f, ann.pointed.g, ann.pointed.x, rmsMemory)
            cost
        }
    }

    /** KANN組み込みのモデルI/O(`kann_save`)で学習済み重みをバイナリファイルに保存する */
    fun save(path: String) {
        kann_save(path, ann)
    }

    fun delete() {
        nativeHeap.free(rmsMemory)
        kann_delete(ann)
    }

    companion object {
        /**
         * [trainStep] で `kann_unroll_array` に渡す展開長の上限。実測でこれを超えると
         * (9以上)KANN側の実装依存の不具合でクラッシュするため、超える場合は直近の
         * トークンだけに切り詰める。[evaluate] はこの上限を受けない。
         */
        const val MAX_TRAIN_UNROLL_LEN = 8

        /**
         * [save] で保存したファイルを読み込む。グラフ構造ごとKANN組み込みの形式で
         * 保存されているため、[KannValueNetwork] を新規構築する代わりにこれを使う。
         */
        fun load(path: String): KannValueNetwork? {
            val loaded = kann_load(path) ?: return null
            return KannValueNetwork(loaded)
        }

        /**
         * 未学習のグラフを構築する。
         * トークン入力(RNN_TOKEN_DIM, 手番ごとに供給) → GRU(RNN_HIDDEN_DIM)
         * 大域特徴入力(RNN_GLOBAL_DIM, 毎ステップ同じ値を供給) → [GRU出力と結合]
         * → Dense(RNN_DENSE_DIM) → tanh → kann_layer_cost が内部で Dense(1)+tanh を追加し、
         *   KANN_C_CEB_NEG (tanhベースの二値交差エントロピー) で学習可能な形にする。
         */
        private fun buildGraph(): CPointer<kann_t> {
            val tokIn = kann_layer_input(RNN_TOKEN_DIM)
                ?: error("kann_layer_input(token) failed")
            val h = kann_layer_gru(tokIn, RNN_HIDDEN_DIM, KANN_RNN_VAR_H0)
                ?: error("kann_layer_gru failed")
            val globIn = kann_layer_input(RNN_GLOBAL_DIM)
                ?: error("kann_layer_input(global) failed")
            globIn.pointed.ext_label = 1

            val combined = memScoped {
                val parts = allocArray<CPointerVar<kad_node_t>>(2)
                parts[0] = h
                parts[1] = globIn
                kad_concat_array(1, 2, parts)
            } ?: error("kad_concat_array failed")

            val dense = kann_layer_dense(combined, RNN_DENSE_DIM)
                ?: error("kann_layer_dense failed")
            val act = kad_tanh(dense) ?: error("kad_tanh failed")
            val costNode = kann_layer_cost(act, 1, KANN_C_CEB_NEG)
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
