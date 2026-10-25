package bstools.valuerl

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

/**
 * GRU 価値ネットワーク V(s) ∈ [-1, 1] (純Kotlin実装、外部NNライブラリ不使用)。
 *
 *   カードトークン x_t = [能力値 16次元 ; カード番号の埋め込み E[id_t]]
 *   x_1..x_T → GRU(hidden) → h_T
 *   [h_T ; 大域特徴 g] → Dense(dense) → tanh → Dense(1) → tanh
 *
 * カード番号はカード固有の効果と結び付くため、必ず入力に含める (埋め込みは必須)。
 * 番号はハッシュで [embBuckets] 個のバケットに割り当て、バケットごとに [embDim] 次元の
 * 学習可能なベクトルを持つ (語彙ファイル不要で、新しいカードにもそのまま対応できる)。
 *
 * 学習は手書きの完全BPTT (系列長の上限なし) + Adam。埋め込みにも勾配を流す。
 * パラメータはすべて1本の [w] (DoubleArray) に平坦に並べ、オフセットで参照する。
 */
class GruValueNet(
    val baseTokenDim: Int = TOKEN_DIM,
    val globalDim: Int = GLOBAL_DIM,
    val hidden: Int = 32,
    val dense: Int = 32,
    val embBuckets: Int = 256,
    val embDim: Int = 8
) {
    /** GRU に入る1トークンの次元 (能力値 + 埋め込み) */
    val tokenDim = baseTokenDim + embDim

    // ---- パラメータレイアウト ----
    private val nWx = hidden * tokenDim
    private val nUh = hidden * hidden
    private val oWz = 0
    private val oWr = oWz + nWx
    private val oWn = oWr + nWx
    private val oUz = oWn + nWx
    private val oUr = oUz + nUh
    private val oUn = oUr + nUh
    private val oBz = oUn + nUh
    private val oBr = oBz + hidden
    private val oBn = oBr + hidden
    private val concatDim = hidden + globalDim
    private val oD = oBn + hidden
    private val oBd = oD + dense * concatDim
    private val oO = oBd + dense
    private val oBo = oO + dense
    private val oE = oBo + 1
    val paramCount = oE + embBuckets * embDim

    val w = DoubleArray(paramCount)

    fun initRandom(rng: Random) {
        fun fill(off: Int, n: Int, fanIn: Int) {
            val s = sqrt(1.0 / fanIn)
            for (i in 0 until n) w[off + i] = (rng.nextDouble() * 2 - 1) * s
        }
        fill(oWz, nWx, tokenDim); fill(oWr, nWx, tokenDim); fill(oWn, nWx, tokenDim)
        fill(oUz, nUh, hidden); fill(oUr, nUh, hidden); fill(oUn, nUh, hidden)
        for (i in 0 until hidden) { w[oBz + i] = 0.0; w[oBr + i] = 0.0; w[oBn + i] = 0.0 }
        fill(oD, dense * concatDim, concatDim)
        for (i in 0 until dense) w[oBd + i] = 0.0
        // 出力層は小さく初期化: 未学習時の評価値を0付近に揃え、特定の手(ステップ終了等)への偏りを抑える
        for (i in 0 until dense) w[oO + i] = (rng.nextDouble() * 2 - 1) * 0.01
        w[oBo] = 0.0
        for (i in 0 until embBuckets * embDim) w[oE + i] = (rng.nextDouble() * 2 - 1) * 0.1
    }

    fun copy(): GruValueNet =
        GruValueNet(baseTokenDim, globalDim, hidden, dense, embBuckets, embDim).also { w.copyInto(it.w) }

    /** カード番号のハッシュ値 → 埋め込みバケット */
    private fun bucket(cardHash: Int): Int = ((cardHash % embBuckets) + embBuckets) % embBuckets

    /** k 番目のトークンの GRU 入力 (能力値 + 埋め込み) を [out] に書き込む */
    private fun input(f: Features, k: Int, out: DoubleArray) {
        f.tokens[k].copyInto(out, 0, 0, baseTokenDim)
        val e = oE + bucket(f.cardIds[k]) * embDim
        for (j in 0 until embDim) out[baseTokenDim + j] = w[e + j]
    }

    // ===================== 順伝播 =====================

    private fun sigmoid(x: Double) = 1.0 / (1.0 + exp(-x))

    /** GRU 1ステップ。キャッシュ用に z, r, n も返す (null なら書き込まない) */
    private fun gruStep(x: DoubleArray, h: DoubleArray, out: DoubleArray,
                        zs: DoubleArray?, rs: DoubleArray?, ns: DoubleArray?) {
        val r = DoubleArray(hidden)
        val z = DoubleArray(hidden)
        for (i in 0 until hidden) {
            var az = w[oBz + i]
            var ar = w[oBr + i]
            val wxRow = i * tokenDim
            for (j in 0 until tokenDim) {
                val xj = x[j]
                if (xj == 0.0) continue
                az += w[oWz + wxRow + j] * xj
                ar += w[oWr + wxRow + j] * xj
            }
            val uRow = i * hidden
            for (j in 0 until hidden) {
                val hj = h[j]
                az += w[oUz + uRow + j] * hj
                ar += w[oUr + uRow + j] * hj
            }
            z[i] = sigmoid(az)
            r[i] = sigmoid(ar)
        }
        for (i in 0 until hidden) {
            var an = w[oBn + i]
            val wxRow = i * tokenDim
            for (j in 0 until tokenDim) {
                val xj = x[j]
                if (xj != 0.0) an += w[oWn + wxRow + j] * xj
            }
            val uRow = i * hidden
            for (j in 0 until hidden) an += w[oUn + uRow + j] * (r[j] * h[j])
            val n = tanh(an)
            out[i] = (1.0 - z[i]) * h[i] + z[i] * n
            if (ns != null) ns[i] = n
        }
        if (zs != null) z.copyInto(zs)
        if (rs != null) r.copyInto(rs)
    }

    /** 頭部 (Dense→tanh→出力) の順伝播。hiddenAct を指定すれば中間活性を書き込む */
    private fun head(h: DoubleArray, g: DoubleArray, hiddenAct: DoubleArray?): Double {
        var out = w[oBo]
        for (i in 0 until dense) {
            var a = w[oBd + i]
            val row = oD + i * concatDim
            for (j in 0 until hidden) a += w[row + j] * h[j]
            for (j in 0 until globalDim) a += w[row + hidden + j] * g[j]
            val t = tanh(a)
            if (hiddenAct != null) hiddenAct[i] = t
            out += w[oO + i] * t
        }
        return tanh(out)
    }

    fun evaluate(f: Features): Double {
        var h = DoubleArray(hidden)
        var nh = DoubleArray(hidden)
        val x = DoubleArray(tokenDim)
        for (k in f.tokens.indices) {
            input(f, k, x)
            gruStep(x, h, nh, null, null, null)
            val t = h; h = nh; nh = t
        }
        return head(h, f.globals, null)
    }

    /**
     * 1手先読みの候補群をまとめて評価する。トークン列 (能力値とカード番号) の接頭辞が同じ候補同士で
     * GRUの隠れ状態を共有する (ローリングハッシュで接頭辞を識別)。結果は [evaluate] と同一。
     * キャッシュはこの呼び出しの中だけで使い、ネットワークをまたいで持ち越さない。
     */
    fun evaluateBatch(batch: List<Features>): DoubleArray {
        val cache = HashMap<Long, DoubleArray>()
        val x = DoubleArray(tokenDim)
        return DoubleArray(batch.size) { bi ->
            val f = batch[bi]
            var key = 1469598103934665603L
            var h = DoubleArray(hidden)
            for ((k, tok) in f.tokens.withIndex()) {
                for (v in tok) key = (key xor v.toRawBits()) * 1099511628211L
                key = (key xor f.cardIds[k].toLong()) * 1099511628211L
                key = key * 31 + 7
                val cached = cache[key]
                if (cached != null) {
                    h = cached
                } else {
                    val nh = DoubleArray(hidden)
                    input(f, k, x)
                    gruStep(x, h, nh, null, null, null)
                    cache[key] = nh
                    h = nh
                }
            }
            head(h, f.globals, null)
        }
    }

    // ===================== 逆伝播 (完全BPTT) =====================

    /**
     * 1サンプルの損失 0.5·(V − target)² の勾配を [grad] に加算し、損失を返す。
     * 埋め込み (カード番号) にも勾配を流す。
     */
    fun accumulateGradient(f: Features, target: Double, grad: DoubleArray): Double {
        val t = f.tokens.size
        val xs = Array(t) { k -> DoubleArray(tokenDim).also { input(f, k, it) } }
        val hs = Array(t + 1) { DoubleArray(hidden) }
        val zs = Array(t) { DoubleArray(hidden) }
        val rs = Array(t) { DoubleArray(hidden) }
        val ns = Array(t) { DoubleArray(hidden) }
        for (k in 0 until t) gruStep(xs[k], hs[k], hs[k + 1], zs[k], rs[k], ns[k])
        val hT = hs[t]
        val act = DoubleArray(dense)
        val v = head(hT, f.globals, act)
        val err = v - target
        val loss = 0.5 * err * err

        // 出力 tanh
        val dOut = err * (1.0 - v * v)
        grad[oBo] += dOut
        val dh = DoubleArray(hidden)
        for (i in 0 until dense) {
            grad[oO + i] += dOut * act[i]
            val da = dOut * w[oO + i] * (1.0 - act[i] * act[i])
            grad[oBd + i] += da
            val row = oD + i * concatDim
            for (j in 0 until hidden) {
                grad[row + j] += da * hT[j]
                dh[j] += da * w[row + j]
            }
            for (j in 0 until globalDim) grad[row + hidden + j] += da * f.globals[j]
        }

        // GRU を時間方向に逆伝播
        val daz = DoubleArray(hidden)
        val dar = DoubleArray(hidden)
        val dan = DoubleArray(hidden)
        val drh = DoubleArray(hidden)
        val dxEmb = DoubleArray(embDim)
        for (k in t - 1 downTo 0) {
            val x = xs[k]
            val hp = hs[k]
            val z = zs[k]; val r = rs[k]; val n = ns[k]
            val dhPrev = DoubleArray(hidden)
            dxEmb.fill(0.0)
            for (i in 0 until hidden) {
                val d = dh[i]
                dan[i] = d * z[i] * (1.0 - n[i] * n[i])
                daz[i] = d * (n[i] - hp[i]) * z[i] * (1.0 - z[i])
                dhPrev[i] = d * (1.0 - z[i])
            }
            // 候補 n: a_n = Wn x + Un (r⊙h) + bn
            for (j in 0 until hidden) drh[j] = 0.0
            for (i in 0 until hidden) {
                val di = dan[i]
                if (di == 0.0) continue
                grad[oBn + i] += di
                val wxRow = i * tokenDim
                for (j in 0 until tokenDim) if (x[j] != 0.0) grad[oWn + wxRow + j] += di * x[j]
                for (j in 0 until embDim) dxEmb[j] += di * w[oWn + wxRow + baseTokenDim + j]
                val uRow = i * hidden
                for (j in 0 until hidden) {
                    grad[oUn + uRow + j] += di * r[j] * hp[j]
                    drh[j] += di * w[oUn + uRow + j]
                }
            }
            for (j in 0 until hidden) {
                dar[j] = drh[j] * hp[j] * r[j] * (1.0 - r[j])
                dhPrev[j] += drh[j] * r[j]
            }
            // ゲート z, r
            for (i in 0 until hidden) {
                val dz = daz[i]
                val dr = dar[i]
                grad[oBz + i] += dz
                grad[oBr + i] += dr
                val wxRow = i * tokenDim
                for (j in 0 until tokenDim) {
                    val xj = x[j]
                    if (xj == 0.0) continue
                    grad[oWz + wxRow + j] += dz * xj
                    grad[oWr + wxRow + j] += dr * xj
                }
                for (j in 0 until embDim) {
                    dxEmb[j] += dz * w[oWz + wxRow + baseTokenDim + j] + dr * w[oWr + wxRow + baseTokenDim + j]
                }
                val uRow = i * hidden
                for (j in 0 until hidden) {
                    grad[oUz + uRow + j] += dz * hp[j]
                    grad[oUr + uRow + j] += dr * hp[j]
                    dhPrev[j] += dz * w[oUz + uRow + j] + dr * w[oUr + uRow + j]
                }
            }
            // 埋め込み: このトークンのカード番号のバケットへ
            val e = oE + bucket(f.cardIds[k]) * embDim
            for (j in 0 until embDim) grad[e + j] += dxEmb[j]
            dhPrev.copyInto(dh)
        }
        return loss
    }

    // ===================== 保存・読込 =====================

    @OptIn(ExperimentalForeignApi::class)
    fun save(path: String): Boolean {
        val f = fopen(path, "wb") ?: return false
        try {
            val header = intArrayOf(MAGIC, VERSION, baseTokenDim, globalDim, hidden, dense, embBuckets, embDim, paramCount)
            header.usePinned { fwrite(it.addressOf(0), 4u, header.size.convert(), f) }
            w.usePinned { fwrite(it.addressOf(0), 8u, w.size.convert(), f) }
        } finally {
            fclose(f)
        }
        return true
    }

    companion object {
        private const val MAGIC = 0x4E565342 // "BSVN"
        private const val VERSION = 2

        fun random(seed: Long, hidden: Int = 32, dense: Int = 32, embBuckets: Int = 256, embDim: Int = 8): GruValueNet =
            GruValueNet(hidden = hidden, dense = dense, embBuckets = embBuckets, embDim = embDim)
                .also { it.initRandom(Random(seed)) }

        /**
         * 重みファイルを読み込む。カード番号の埋め込みを持たない旧形式 (version 1) は、
         * 埋め込み部分を追加して読み込む: GRU の埋め込み入力の重みは 0、埋め込みは小さな乱数で初期化するため、
         * 読み込み直後の評価値は旧ネットワークと完全に同じになる。
         */
        @OptIn(ExperimentalForeignApi::class)
        fun load(path: String): GruValueNet? {
            val f = fopen(path, "rb") ?: return null
            try {
                val head2 = IntArray(2)
                if (head2.usePinned { fread(it.addressOf(0), 4u, 2u, f) }.toInt() != 2 || head2[0] != MAGIC) return null
                when (head2[1]) {
                    2 -> {
                        val h = IntArray(7)
                        if (h.usePinned { fread(it.addressOf(0), 4u, 7u, f) }.toInt() != 7) return null
                        val net = GruValueNet(h[0], h[1], h[2], h[3], h[4], h[5])
                        if (net.paramCount != h[6] || net.baseTokenDim != TOKEN_DIM || net.globalDim != GLOBAL_DIM) return null
                        val n = net.w.usePinned { fread(it.addressOf(0), 8u, net.paramCount.convert(), f) }
                        return if (n.toInt() == net.paramCount) net else null
                    }
                    1 -> {
                        val h = IntArray(5) // tokenDim, globalDim, hidden, dense, paramCount
                        if (h.usePinned { fread(it.addressOf(0), 4u, 5u, f) }.toInt() != 5) return null
                        if (h[0] != TOKEN_DIM || h[1] != GLOBAL_DIM) return null
                        val old = DoubleArray(h[4])
                        if (old.usePinned { fread(it.addressOf(0), 8u, old.size.convert(), f) }.toInt() != old.size) return null
                        return upgradeV1(old, h[2], h[3])
                    }
                    else -> return null
                }
            } finally {
                fclose(f)
            }
        }

        /** 旧形式の重み列 (埋め込みなし) を、埋め込み付きのレイアウトへ写す */
        private fun upgradeV1(old: DoubleArray, hidden: Int, dense: Int): GruValueNet? {
            val net = random(0, hidden, dense)
            val base = TOKEN_DIM
            val td = net.tokenDim
            val oldNWx = hidden * base
            val oldGruX = 3 * oldNWx
            val tailOld = old.size - oldGruX // U, b, Dense, 出力 (レイアウトは新旧で同じ)
            if (tailOld != net.oE - 3 * hidden * td) return null
            for (g in 0 until 3) for (i in 0 until hidden) {
                for (j in 0 until base) net.w[g * hidden * td + i * td + j] = old[g * oldNWx + i * base + j]
                for (j in base until td) net.w[g * hidden * td + i * td + j] = 0.0
            }
            old.copyInto(net.w, 3 * hidden * td, oldGruX, old.size)
            return net
        }
    }
}

/** Adam 最適化器 (パラメータと同じ平坦レイアウトでモーメントを保持) */
class Adam(size: Int, var lr: Double, private val beta1: Double = 0.9, private val beta2: Double = 0.999,
           private val eps: Double = 1e-8, private val clip: Double = 1.0) {
    private val m = DoubleArray(size)
    private val v = DoubleArray(size)
    private var t = 0

    /** grad は平均勾配。全体ノルムが [clip] を超えたら縮小してから更新する */
    fun step(w: DoubleArray, grad: DoubleArray) {
        var norm2 = 0.0
        for (g in grad) norm2 += g * g
        val norm = sqrt(norm2)
        val scale = if (norm > clip) clip / norm else 1.0
        t++
        val bc1 = 1.0 - beta1.pow(t)
        val bc2 = 1.0 - beta2.pow(t)
        for (i in w.indices) {
            val g = grad[i] * scale
            m[i] = beta1 * m[i] + (1 - beta1) * g
            v[i] = beta2 * v[i] + (1 - beta2) * g * g
            w[i] -= lr * (m[i] / bc1) / (sqrt(v[i] / bc2) + eps)
        }
    }
}
