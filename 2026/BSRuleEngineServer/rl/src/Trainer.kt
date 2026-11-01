package bstools.valuerl

import kotlin.random.Random

/** 重みファイルの拡張子 */
const val NN_EXT = "gru.weight"

/**
 * 自己対戦 TD(λ) による価値ネットワークの強化学習。
 *
 * 1イテレーション:
 *  1. 現行ネット同士で自己対戦 (ε-greedy 探索) し、各プレイヤーの afterstate 列を記録
 *  2. 各列について、現行ネットの値 V(s_{t+1}) と最終報酬 z
 *     (勝+1 / 負-1 / 手数上限の引分 [TrainConfig.drawReward]) から
 *     λ-return を後ろ向きに計算: G_{T-1} = z, G_t = (1-λ)·V(s_{t+1}) + λ·G_{t+1}
 *  3. (特徴, G) をリプレイバッファへ追加し、一様サンプリングしたミニバッチで
 *     損失 0.5(V-G)² を BPTT + Adam で最小化。更新後はバッファを空にする
 *     (局面と目標値はそれを作ったネットワークでのみ使い、ネットワークをまたいで持ち越さない)
 *  4. [TrainConfig.evalEvery] ごとに「現行 vs 最良」の直接対戦と、歴代最良モデルのリーグ戦 (Elo) で評価し、
 *     両方の条件を満たせば最良を更新してリーグに加える ([League] 参照)
 *
 * 評価は価値ネットワークのみ (手書きヒューリスティックによる事前学習・比較は持たない)。
 * 旧実装(ES山登り)と違い勾配で全パラメータを同時に動かすため、勝率が50%に張り付く
 * 「常にパス」局所解でも、探索で得た勝敗の差が直接 V に反映される。
 */
data class TrainConfig(
    val iterations: Int = 50,
    val gamesPerIter: Int = 16,
    val lambda: Double = 0.7,
    val lr: Double = 1e-3,
    val updatesPerIter: Int = 200,
    val batchSize: Int = 32,
    val epsilonStart: Double = 0.15,
    val epsilonEnd: Double = 0.03,
    val evalEvery: Int = 5,
    val evalGames: Int = 20,
    val promote: Double = 0.55,
    /** 自己対戦のソフトマックス温度 (0 なら ε-greedy のみ) */
    val selfTemp: Double = 0.0,
    /** 評価対局のソフトマックス温度。序盤の展開を局ごとに散らし、毎局同じ指し回しになるのを防ぐ */
    val evalTemp: Double = 0.05,
    /** 評価対局で温度を使うターン数 (それ以降は貪欲) */
    val evalTempTurns: Int = 4,
    /**
     * 手数上限による引き分けの報酬 (両者) は、直近の引き分け率に応じて動的に決める:
     *   報酬 = −min(drawMax, drawBase + drawGain × 引き分け率)
     * 引き分けが増えるほど引き延ばしを強く罰し、決着がつく状況では罰を弱める。
     * 引き分け率は自己対戦1局ごとの指数移動平均 (係数 [drawEmaAlpha])。
     */
    val drawBase: Double = 0.1,
    val drawGain: Double = 0.8,
    val drawMax: Double = 0.9,
    val drawEmaAlpha: Double = 0.1,
    /**
     * 1手ごとの報酬 −c (選択の余地があった自分の手1回につき)。手数の上限は設けずに、
     * 評価がほとんど変わらないコア移動などを延々と続ける引き延ばしを不利にする。
     * 1局で自分が選ぶ手は約200回なので、0.001 なら引き延ばした局で合計 −0.2 程度。
     */
    val stepPenalty: Double = 0.0,
    /**
     * ミニマックス探索の深さ (1 = 1手先読み)。自己対戦・評価対局・リーグのすべてで使う (全員同じ深さで比べる)。
     * 2 以上のときは λ-return の「次の局面の評価値」に、現行ネットの評価値ではなく対局中の探索値を使う。
     */
    val depth: Int = 1,
    /** 探索の幅 (既定1 = 1手先読みで最善の1手だけ深く読む、0 = 制限なし)。深さ2以上で、各局面で深く読む手を1手先読みの上位 width 手に絞る。全対局共通 */
    val width: Int = 1,
    /** リーグの1メンバーあたりの対戦数 */
    val leagueGames: Int = 10,
    /** リーグの最大人数 (ランダム方策を含む) */
    val leagueSize: Int = 8,
    val seed: Long = 1,
    val game: GameConfig = GameConfig()
)

/**
 * 1反復分の (局面, 目標値) を保持するバッファ。上限は設けず、その反復の局面はすべて残す。
 * 目標値は現在のネットワークで計算しているため、ネットワークを更新したら [clear] で破棄する。
 */
class ReplayBuffer {
    private val xs = ArrayList<Features>()
    private val ys = ArrayList<Double>()
    val size get() = xs.size

    fun add(x: Features, y: Double) { xs.add(x); ys.add(y) }

    fun sample(rng: Random): Pair<Features, Double> { val i = rng.nextInt(xs.size); return xs[i] to ys[i] }

    fun clear() { xs.clear(); ys.clear() }
}

/**
 * λ-return を計算する。[values] は各 afterstate の現行評価値 (または探索値)、[z] は最終報酬。
 * [stepPenalty] は1手ごとの報酬 −c: afterstate t から次の自分の手 (または終局) までに1回ずつ引く。
 * 長い手順ほど目標値が下がるので、評価がほとんど変わらない手を延々と続ける方策が不利になる。
 * 目標値は価値ネットの出力範囲 [-1, 1] に切り詰める。
 */
fun lambdaReturns(values: DoubleArray, z: Double, lambda: Double, stepPenalty: Double = 0.0): DoubleArray {
    val n = values.size
    val g = DoubleArray(n)
    if (n == 0) return g
    g[n - 1] = (z - stepPenalty).coerceIn(-1.0, 1.0)
    for (t in n - 2 downTo 0) g[t] = (-stepPenalty + (1 - lambda) * values[t + 1] + lambda * g[t + 1]).coerceIn(-1.0, 1.0)
    return g
}

/**
 * @param base 出力の接頭辞 (例: weights/init_g1)。最新=<base>.gru.weight、最良=<base>-best.gru.weight、
 *             リーグ加入時のスナップショット=<base>-p<番号>.gru.weight
 * @param poolPaths リーグに初期加入させる既存の重みファイル (過去の最良モデル等)
 */
class Trainer(var net: GruValueNet, val cfg: TrainConfig, private val base: String, private val poolPaths: List<String> = emptyList()) {
    private val outPath = "$base.$NN_EXT"
    private val bestPath = "$base-best.$NN_EXT"
    private val rng = Random(cfg.seed)
    private val adam = Adam(net.paramCount, cfg.lr)
    private val buffer = ReplayBuffer()
    private var best = net.copy()
    private var bestElo = 0.0
    private val league = League(cfg.leagueGames, cfg.leagueSize, cfg.game)
    private var snapshots = 0
    /** 自己対戦の引き分け率 (指数移動平均) */
    private var drawRate = 0.0

    /** 現在の引き分け率から決まる、手数上限引き分けの報酬 (両者) */
    private fun drawReward(): Double = -minOf(cfg.drawMax, cfg.drawBase + cfg.drawGain * drawRate)

    /** 1局の軌跡をλ-return付きでバッファへ入れる */
    private fun ingest(r: GameResult) {
        drawRate = (1 - cfg.drawEmaAlpha) * drawRate + cfg.drawEmaAlpha * (if (r.winner == null) 1.0 else 0.0)
        val drawZ = drawReward()
        for ((pid, traj) in r.trajectories) {
            if (traj.isEmpty()) continue
            // 手数上限の引き分けは両者に負の報酬 (引き分けが多いほど強い): 決着を避けて引き延ばす方策を学ばせない
            val z = when (r.winner) { null -> drawZ; pid -> 1.0; else -> -1.0 }
            val searched = r.searchValues[pid]
            val values = if (cfg.depth > 1 && searched != null && searched.size == traj.size)
                DoubleArray(traj.size) { searched[it] }
            else DoubleArray(traj.size) { net.evaluate(traj[it]) }
            val targets = lambdaReturns(values, z, cfg.lambda, cfg.stepPenalty)
            for (i in traj.indices) buffer.add(traj[i], targets[i])
        }
    }

    private fun update(): Double {
        if (buffer.size == 0) return 0.0
        val grad = DoubleArray(net.paramCount)
        var lossSum = 0.0
        repeat(cfg.updatesPerIter) {
            grad.fill(0.0)
            var loss = 0.0
            repeat(cfg.batchSize) {
                val (x, y) = buffer.sample(rng)
                loss += net.accumulateGradient(x, y, grad)
            }
            val inv = 1.0 / cfg.batchSize
            for (i in grad.indices) grad[i] *= inv
            adam.step(net.w, grad)
            lossSum += loss * inv
        }
        return lossSum / cfg.updatesPerIter
    }

    fun run() {
        println("=== GRU価値ネット TD(λ) 自己対戦学習 ===")
        println("パラメータ数=${net.paramCount} 反復=${cfg.iterations} 局/反復=${cfg.gamesPerIter} λ=${cfg.lambda} lr=${cfg.lr} " +
            "手数上限=${cfg.game.maxSteps} 引分報酬=−min(${fmt(cfg.drawMax, 2)}, ${fmt(cfg.drawBase, 2)}+${fmt(cfg.drawGain, 2)}×引分率) " +
            "評価温度=${fmt(cfg.evalTemp, 3)}(${cfg.evalTempTurns}ターン) リーグ=${cfg.leagueGames}局×最大${cfg.leagueSize} " +
            "1手の報酬=−${fmt(cfg.stepPenalty, 4)} 探索 深さ=${cfg.depth} 幅=${if (cfg.width > 0) cfg.width.toString() else "制限なし"}")
        println("埋め込み=${net.embBuckets}バケット×${net.embDim}次元 (カード番号を入力に使用) 列挙=$actionSet 並列=${Parallel.threads}")
        best.save(bestPath)

        // リーグ初期化: 指定の既存モデル → 開始時点のモデル の順に、その時点のリーグで Elo を測って加入
        for ((i, p) in poolPaths.withIndex()) {
            val m = GruValueNet.load(p) ?: error("リーグ用の重みファイルを読み込めません: $p")
            val res = league.rate(evalAgent(m, p), cfg.seed * 31 + i)
            league.add(LeagueMember(p.substringAfterLast('/'), evalAgent(m, p), res.elo))
            println("リーグ加入: ${p.substringAfterLast('/')} Elo=${fmt(res.elo, 0)}")
        }
        val start = league.rate(evalAgent(best, "start"), cfg.seed * 31 + 999)
        bestElo = start.elo
        league.add(LeagueMember("start", evalAgent(best.copy(), "start"), bestElo))
        println("リーグ加入: start(開始時点) Elo=${fmt(bestElo, 0)}")

        val agent = NetAgent(net, "current", temperature = cfg.selfTemp, depth = cfg.depth, width = cfg.width)
        for (it in 1..cfg.iterations) {
            val frac = if (cfg.iterations <= 1) 1.0 else (it - 1).toDouble() / (cfg.iterations - 1)
            agent.epsilon = cfg.epsilonStart + (cfg.epsilonEnd - cfg.epsilonStart) * frac
            var w1 = 0; var w2 = 0; var dr = 0; var steps = 0L
            // 自己対戦は並列に指す (ネットは読むだけ)。学習データへの取り込みは局の順番どおりに行う
            val seeds = List(cfg.gamesPerIter) { rng.nextLong() to rng.nextLong() }
            for (r in playGamesParallel(seeds, cfg.game, record = true) { agent to agent }) {
                when (r.winner) { 1 -> w1++; 2 -> w2++; else -> dr++ }
                steps += r.steps
                ingest(r)
            }
            val samples = buffer.size
            val loss = update()
            buffer.clear() // ネットワークが変わったので、旧ネットワークで作った目標値は破棄
            net.save(outPath)
            println("反復$it/${cfg.iterations}: ε=${fmt(agent.epsilon, 3)} 先手勝=$w1 後手勝=$w2 引分=$dr 平均手数=${steps / cfg.gamesPerIter} " +
                "学習局面=$samples 引分率=${pct(drawRate)} 引分報酬=${fmt(drawReward(), 3)} 損失=${fmt(loss)}")

            if (it % cfg.evalEvery == 0 || it == cfg.iterations) evaluate(it)
        }
        println("=== 学習完了: 最新=$outPath / 最良=$bestPath ===")
    }

    /** 評価対局用のエージェント (序盤だけ温度付き、以降は貪欲)。比較を公平にするため全員同じ設定 */
    private fun evalAgent(n: GruValueNet, name: String) =
        NetAgent(n, name, temperature = cfg.evalTemp, tempTurns = cfg.evalTempTurns, depth = cfg.depth, width = cfg.width)

    /**
     * 最良の更新条件 (両方を満たすこと):
     *  1. 直前の最良との直接対戦スコア ≥ [TrainConfig.promote]
     *     ただし直接対戦が全局引き分け (決着0局) なら判定材料がないため、この条件は問わない
     *  2. リーグ全体で測った Elo が最良の Elo を上回る (循環的な「直前の最良にだけ強い」を弾く)
     * 更新したモデルはスナップショットとして保存し、リーグに加入させる。
     */
    private fun evaluate(it: Int) {
        val evalSeed = cfg.seed * 1_000_003 + it
        val cur = evalAgent(net, "current")
        val vsBest = arena(cur, evalAgent(best, "best"), cfg.evalGames, evalSeed, cfg.game)
        val lg = league.rate(cur, evalSeed + 1)
        val allDraws = vsBest.aWins + vsBest.bWins == 0
        val cond1 = allDraws || vsBest.score >= cfg.promote
        val promoted = cond1 && lg.elo > bestElo
        val table = lg.rows.joinToString(" ") { (m, st) -> "${m.name}(${fmt(m.elo, 0)}):${pct(st.score)}" }
        println("  評価: vs最良 ${vsBest.aWins}勝${vsBest.bWins}敗${vsBest.draws}分 (${pct(vsBest.score)})" +
            (if (allDraws) " [全局引き分けのため Elo のみで判定]" else "") + " / " +
            "Elo ${fmt(lg.elo, 0)} (最良 ${fmt(bestElo, 0)})" + (if (promoted) " → 最良を更新" else ""))
        println("    リーグ: $table")
        if (promoted) {
            best = net.copy()
            bestElo = lg.elo
            best.save(bestPath)
            snapshots++
            val snap = "$base-p$snapshots.$NN_EXT"
            best.save(snap)
            league.add(LeagueMember("p$snapshots@$it", evalAgent(best.copy(), "p$snapshots"), lg.elo))
            println("    スナップショット保存・リーグ加入: $snap")
        }
    }
}
