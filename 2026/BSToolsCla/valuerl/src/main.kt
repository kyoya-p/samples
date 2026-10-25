import bstools.model.*
import bstools.valuerl.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

/**
 * BSToolsCla valuerl: GRU価値ネットワーク + 自己対戦TD(λ)強化学習 CLI
 *
 *   selftest                         勾配検査・保存読込・対局の動作確認
 *   train  [--iters N] [--games N] [--lambda L] [--lr R] [--updates N] [--batch N]
 *          [--eps-start E] [--eps-end E] [--eval-every N] [--eval-games N]
 *          [--promote P] [--seed S] [--max-steps M]
 *          [--hidden H] [--dense D] [--emb-buckets B] [--emb-dim E]   (新規作成時のみ)
 *          [--self-temp T] [--eval-temp T] [--eval-temp-turns N]
 *          [--draw-base B] [--draw-gain G] [--draw-max M] [--draw-ema A] [--step-penalty C]
 *          [--league-games N] [--league-size N] [--pool f1,f2,...]
 *          [--resume <file>] [--out <file>] [--deck1 f] [--deck2 f]
 *   match  --a <file|random> --b <file|random> [--games N] [--seed S] [--max-steps M] [--log f]
 *          [--temp T] [--temp-turns N]
 *   branch [--weights f] [--games N] [--seed S] [--depth D]
 *                                    1手ごとの候補数と、D手先まで読んだ場合の評価局面数・時間を計測
 *   league --models f1,f2,... [--games N] [--seed S] [--temp T] [--temp-turns N] [--max-steps M]
 *                                    総当たり戦 (全組み合わせを先後交互で N 局ずつ) と Elo の同時推定
 *   depthbench [--weights f] [--max-depth N] [--widths 0,3,5] [--games N] [--every K] [--budget 秒] [--seed S]
 *                                    同じ局面群でミニマックスの深さ 1..N × 幅 の思考時間・評価局面数・手の一致率を計測
 *   (train / match / league は --depth N で探索の深さ、--width k で探索の幅 (既定1、0=制限なし) を指定。
 *    match は --depth-a / --depth-b / --width-a / --width-b も可)
 *   inspect <file>                   重みファイルの統計
 *   value  <file> [--seed S]         初期局面の評価値と候補手の評価一覧
 *
 * 共通: --threads N (同時に指す局数。既定は論理プロセッサ数。自己対戦・評価対局・リーグ戦・match・league を並列化)
 *       --actions level|all|compact|full (既定 level: Lv単位の正規形列挙 = 各カードのLvとソウルコアの場所の
 *       全組み合わせに到達でき、循環しない。all は全合法手 (終盤に件数が爆発)、compact は圧縮列挙、full は件数上限ありの従来列挙)
 *
 * 重みファイルは名前だけ指定すると weights/ 配下として扱う。
 * 命名規則: weights/<派生元>_<派生先>.gru.weight (--out 省略時は自動採番)
 */
fun main(args: Array<String>) {
    val cmd = args.firstOrNull() ?: "help"
    val opts = Opts(args.drop(1))
    actionSet = ActionSet.parse(opts.str("--actions"))
    opts.str("--threads")?.toIntOrNull()?.let { Parallel.threads = it.coerceIn(1, 64) }
    when (cmd) {
        "selftest" -> selfTest()
        "train" -> train(opts)
        "match" -> match(opts)
        "inspect" -> inspect(opts.positional.firstOrNull() ?: error("inspect <file>"))
        "value" -> value(opts)
        "branch" -> branch(opts)
        "league" -> roundRobin(opts)
        "depthbench" -> depthBench(opts)
        else -> println("使い方: valuerl <selftest|train|match|inspect|value> [options]")
    }
}

class Opts(list: List<String>) {
    private val map = HashMap<String, String>()
    val positional = ArrayList<String>()
    init {
        var i = 0
        while (i < list.size) {
            val a = list[i]
            if (a.startsWith("--") && i + 1 < list.size) { map[a] = list[i + 1]; i += 2 }
            else { positional.add(a); i++ }
        }
    }
    fun str(k: String) = map[k]
    fun int(k: String, d: Int) = map[k]?.toIntOrNull() ?: d
    fun long(k: String, d: Long) = map[k]?.toLongOrNull() ?: d
    fun dbl(k: String, d: Double) = map[k]?.toDoubleOrNull() ?: d
}

const val WEIGHTS_DIR = "weights"
const val NN_TYPE = "gru"

fun weightPath(p: String): String = if (p.contains('/') || p.contains('\\')) p else "$WEIGHTS_DIR/$p"

/** "<from>_<to>.gru.weight" → 次の名前。to の末尾数字を+1する (例: g1_g2 → g2_g3) */
fun nextWeightName(resume: String?): String {
    if (resume == null) return "init_g1"
    // "-best" / "-p3" などの派生サフィックスは名前の一部とみなさない
    val base = resume.substringAfterLast('/').substringAfterLast('\\').substringBefore('.').substringBefore('-')
    val to = base.substringAfter('_', base)
    val digits = to.takeLastWhile { it.isDigit() }
    val next = if (digits.isEmpty()) "${to}1" else to.dropLast(digits.length) + (digits.toInt() + 1)
    return "${to}_$next"
}

fun loadOrDie(p: String): GruValueNet =
    GruValueNet.load(weightPath(p)) ?: error("重みファイルを読み込めません(形式・次元不一致含む): ${weightPath(p)}")

fun gameCfg(o: Opts) = GameConfig(
    maxSteps = o.int("--max-steps", 1500),
    deck1 = o.str("--deck1") ?: o.str("--deck") ?: "deck-kogyo.yaml",
    deck2 = o.str("--deck2") ?: o.str("--deck") ?: "deck-kogyo.yaml"
)

fun train(o: Opts) {
    val resume = o.str("--resume")
    val net = resume?.let { loadOrDie(it) }
        ?: GruValueNet.random(o.long("--seed", 1), o.int("--hidden", 32), o.int("--dense", 32),
            o.int("--emb-buckets", 256), o.int("--emb-dim", 8))
    val name = o.str("--out")?.substringAfterLast('/')?.substringAfterLast('\\')?.substringBefore('.') ?: nextWeightName(resume)
    val pool = o.str("--pool")?.split(',')?.filter { it.isNotBlank() }?.map { weightPath(it.trim()) } ?: emptyList()
    mkdirWeights()
    requireSearch(o.int("--depth", 1), o.int("--width", 1))
    val cfg = TrainConfig(
        iterations = o.int("--iters", 50),
        gamesPerIter = o.int("--games", 16),
        lambda = o.dbl("--lambda", 0.7),
        lr = o.dbl("--lr", 1e-3),
        updatesPerIter = o.int("--updates", 200),
        batchSize = o.int("--batch", 32),
        epsilonStart = o.dbl("--eps-start", 0.15),
        epsilonEnd = o.dbl("--eps-end", 0.03),
        evalEvery = o.int("--eval-every", 5),
        evalGames = o.int("--eval-games", 20),
        promote = o.dbl("--promote", 0.55),
        selfTemp = o.dbl("--self-temp", 0.0),
        evalTemp = o.dbl("--eval-temp", 0.05),
        evalTempTurns = o.int("--eval-temp-turns", 4),
        drawBase = o.dbl("--draw-base", 0.1),
        drawGain = o.dbl("--draw-gain", 0.8),
        drawMax = o.dbl("--draw-max", 0.9),
        drawEmaAlpha = o.dbl("--draw-ema", 0.1),
        stepPenalty = o.dbl("--step-penalty", 0.0),
        depth = o.int("--depth", 1),
        width = o.int("--width", 1),
        leagueGames = o.int("--league-games", 10),
        leagueSize = o.int("--league-size", 8),
        seed = o.long("--seed", 1),
        game = gameCfg(o)
    )
    if (resume != null) println("初期重み: ${weightPath(resume)}")
    Trainer(net, cfg, weightPath(name), pool).run()
}

fun agentFor(spec: String, label: String, temp: Double, tempTurns: Int, depth: Int = 1, width: Int = 1): Agent = when (spec) {
    "random" -> RandomAgent()
    else -> NetAgent(loadOrDie(spec), label, temperature = temp, tempTurns = tempTurns, depth = depth, width = width)
}

/** 探索設定が不正なら理由を表示して終了する (幅1で深さ1以外など) */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun requireSearch(depth: Int, width: Int, who: String = "") {
    val e = searchSettingsError(depth, width) ?: return
    println("エラー: " + (if (who.isEmpty()) "" else "[$who] ") + e)
    platform.posix.exit(2)
}

fun widthLabel(w: Int) = if (w > 0) "幅$w" else "幅制限なし"

fun match(o: Opts) {
    val aSpec = o.str("--a") ?: error("--a が必要")
    val bSpec = o.str("--b") ?: "random"
    val games = o.int("--games", 20)
    val temp = o.dbl("--temp", 0.0)
    val tempTurns = o.int("--temp-turns", 4)
    val depthA = o.int("--depth-a", o.int("--depth", 1))
    val depthB = o.int("--depth-b", o.int("--depth", 1))
    val widthA = o.int("--width-a", o.int("--width", 1))
    val widthB = o.int("--width-b", o.int("--width", 1))
    requireSearch(depthA, widthA, "A"); requireSearch(depthB, widthB, "B")
    val log = o.str("--log")?.let { GameLog(it) }
    val st = arena(agentFor(aSpec, "A:$aSpec", temp, tempTurns, depthA, widthA), agentFor(bSpec, "B:$bSpec", temp, tempTurns, depthB, widthB),
        games, o.long("--seed", 1), gameCfg(o), log)
    log?.close()
    println("=== $aSpec(深さ$depthA ${widthLabel(widthA)}) vs $bSpec(深さ$depthB ${widthLabel(widthB)}) ($games 局, 先後交互) ===")
    println("A: ${st.aWins}勝 ${st.bWins}敗 ${st.draws}分 / スコア ${pct(st.score)}")
    println("A先手時: ${st.aFirstWins}/${st.aFirstGames}勝  A後手時: ${st.aWins - st.aFirstWins}/${games - st.aFirstGames}勝")
    println("平均決着ターン ${fmt(st.turnSum.toDouble() / games, 1)} / 平均手数 ${fmt(st.stepSum.toDouble() / games, 1)}")
}

fun inspect(p: String) {
    val net = loadOrDie(p)
    val w = net.w
    val mean = w.average()
    var v = 0.0
    for (x in w) v += (x - mean) * (x - mean)
    println("${weightPath(p)}: token=${net.baseTokenDim}+埋め込み${net.embDim} (${net.embBuckets}バケット) global=${net.globalDim} hidden=${net.hidden} dense=${net.dense}")
    println("パラメータ数=${net.paramCount} min=${fmt(w.min())} max=${fmt(w.max())} mean=${fmt(mean)} std=${fmt(kotlin.math.sqrt(v / w.size))}")
}

fun value(o: Opts) {
    val net = o.positional.firstOrNull()?.let { loadOrDie(it) } ?: GruValueNet.random(1)
    val state = createInitialGameState(seed = o.long("--seed", 1))
    println("V(P1視点)=${fmt(net.evaluate(extractFeatures(state, 1)))} V(P2視点)=${fmt(net.evaluate(extractFeatures(state, 2)))}")
    val c = NetAgent(net).choose(state, Random(0))
    for ((d, v) in c.alternatives.sortedByDescending { it.second }) println("  ${fmt(v)}  $d")
    println("=> ${c.action?.detail ?: "ステップ終了"}")
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun mkdirWeights() { platform.posix.mkdir(WEIGHTS_DIR) }

/**
 * 総当たり戦。全ての組み合わせを先後交互で [games] 局ずつ対戦させ、勝敗表と Elo を出す。
 * Elo は全員を同時に最尤推定する (平均を 0 に固定。相手ごとに「0.5勝の仮想1局」を加えて発散を防ぐ)。
 * 評価対局と同じく、序盤だけ温度付きで指し、以降は最善手を選ぶ。
 */
fun roundRobin(o: Opts) {
    val names = (o.str("--models") ?: error("--models a,b,c が必要")).split(',').map { it.trim() }.filter { it.isNotEmpty() }
    require(names.size >= 2) { "モデルは2つ以上指定する" }
    val games = o.int("--games", 20)
    val temp = o.dbl("--temp", 0.05)
    val tempTurns = o.int("--temp-turns", 4)
    val seed = o.long("--seed", 1)
    val cfg = gameCfg(o)
    requireSearch(o.int("--depth", 1), o.int("--width", 1))
    val agents = names.map { NetAgent(loadOrDie(it), it, temperature = temp, tempTurns = tempTurns, depth = o.int("--depth", 1), width = o.int("--width", 1)) }
    val n = names.size
    val w = Array(n) { IntArray(n) }      // w[i][j] = i が j に勝った数
    val d = Array(n) { IntArray(n) }      // 引き分け
    var pair = 0
    println("=== 総当たり戦: ${n}モデル × 各組 ${games}局 (先後交互、序盤${tempTurns}ターン温度${fmt(temp, 3)}) ===")
    for (i in 0 until n) for (j in i + 1 until n) {
        val st = arena(agents[i], agents[j], games, seed * 7919 + pair++, cfg)
        w[i][j] = st.aWins; w[j][i] = st.bWins; d[i][j] = st.draws; d[j][i] = st.draws
        println("  ${names[i]} vs ${names[j]}: ${st.aWins}勝 ${st.bWins}敗 ${st.draws}分")
    }
    // Elo の同時最尤推定 (ニュートン法、平均 0)
    val r = DoubleArray(n)
    val ln10 = kotlin.math.ln(10.0)
    repeat(200) {
        for (i in 0 until n) {
            var s = 0.0; var e = 0.0; var h = 0.0
            for (j in 0 until n) {
                if (i == j) continue
                val g = w[i][j] + w[j][i] + d[i][j] + 1.0
                val score = w[i][j] + 0.5 * d[i][j] + 0.5
                val p = League.expected(r[i], r[j])
                s += score; e += g * p; h += g * p * (1 - p)
            }
            if (h > 0) r[i] += (s - e) / (h * ln10 / 400.0)
        }
        val mean = r.average()
        for (i in 0 until n) r[i] -= mean
    }
    val order = (0 until n).sortedByDescending { r[it] }
    println("=== 順位 (Elo は平均 0 基準) ===")
    for ((rank, i) in order.withIndex()) {
        var win = 0; var lose = 0; var draw = 0
        for (j in 0 until n) if (j != i) { win += w[i][j]; lose += w[j][i]; draw += d[i][j] }
        val total = win + lose + draw
        println("  ${rank + 1}. ${names[i]}  Elo ${fmt(r[i], 0)}  ${win}勝${lose}敗${draw}分  スコア ${pct((win + 0.5 * draw) / maxOf(1, total))}")
    }
    println("=== 勝敗表 (行が対列のスコア) ===")
    for (i in order) {
        println("  ${names[i].padEnd(28)} " + order.joinToString(" ") { j ->
            if (i == j) "   -  " else pct((w[i][j] + 0.5 * d[i][j]) / maxOf(1, w[i][j] + w[j][i] + d[i][j])).padStart(6)
        })
    }
}

/**
 * 自分の手番が続く間 (同じプレイヤーが選ぶ局面) を depth 手先まで全部読んだときに評価する局面数。
 * depth=1 が現在の1手先読み。相手の選択や決着で読みを打ち切る。
 */
private fun lookaheadCount(s: GameState, me: Int, depth: Int): Long {
    val os = optionsOf(s)
    var total = 0L
    for (a in os) {
        total++
        if (depth > 1) {
            val t = afterstate(s, a)
            if (t.winner == null && t.choosingPlayerId == me) total += lookaheadCount(t, me, depth - 1)
        }
    }
    return total
}

/**
 * 選択肢をいくつ評価しているかの計測。学習済みモデル同士で対局し、1手ごとに
 * 候補数 (圧縮列挙 / 全列挙) と、自分の連続手を depth 手先まで読んだ場合の評価局面数、
 * 現在の1手先読みの所要時間を集計する。
 */
fun branch(o: Opts) {
    val net = loadOrDie(o.str("--weights") ?: "g1_g2-best.gru.weight")
    val games = o.int("--games", 10)
    val maxDepth = o.int("--depth", 3)
    val cfg = gameCfg(o)
    val rng = Random(o.long("--seed", 1))
    val agent = NetAgent(net, "m", temperature = 0.05, tempTurns = 4)

    class Acc { var n = 0; var opts = 0L; var maxOpts = 0; var full = 0L; var forced = 0; var nanos = 0L
        val la = LongArray(maxDepth + 1); val laMax = LongArray(maxDepth + 1) }
    val byStep = LinkedHashMap<String, Acc>()
    val all = Acc()
    val optHist = IntArray(12) // 1,2,...,10,11-20,21+

    repeat(games) {
        val state = createInitialGameState(cfg.deck1, cfg.deck2, rng.nextLong(), cfg.format)
        state.visitedPositions.add(positionHash(state))
        var step = 0
        while (state.winner == null && step < cfg.maxSteps) {
            val me = state.choosingPlayerId
            val os = optionsOf(state)
            val full = enumerateActions(state).count { !it.repeatsPosition } + (if (canEndStep(state)) 1 else 0)
            val la = LongArray(maxDepth + 1)
            for (d in 2..maxDepth) la[d] = lookaheadCount(state, me, d)
            val mark = kotlin.time.TimeSource.Monotonic.markNow()
            val choice = agent.choose(state, rng)
            val ns = mark.elapsedNow().inWholeNanoseconds
            for (acc in listOf(all, byStep.getOrPut(state.step.displayName) { Acc() })) {
                acc.n++; acc.opts += os.size; acc.maxOpts = maxOf(acc.maxOpts, os.size); acc.full += full
                if (os.size == 1) acc.forced++ else acc.nanos += ns
                for (d in 2..maxDepth) { acc.la[d] += la[d]; acc.laMax[d] = maxOf(acc.laMax[d], la[d]) }
            }
            optHist[when { os.size <= 10 -> os.size - 1; os.size <= 20 -> 10; else -> 11 }]++
            val msgs = mutableListOf<String>()
            val a = choice.action
            if (a == null) { advanceStep(state, msgs); state.visitedPositions.add(positionHash(state)) }
            else applyActionAndRecord(state, a, msgs)
            step++
        }
    }

    fun avg(x: Long, n: Int) = fmt(x.toDouble() / maxOf(1, n), 1)
    val evalNanos = all.nanos.toDouble() / maxOf(1L, all.opts - all.forced) // 評価1局面あたり (自動進行を除く概算)
    println("=== 選択肢の数の計測: ${games}局 ${all.n}手 (モデル ${o.str("--weights") ?: "g1_g2-best.gru.weight"}) ===")
    println("ステップ | 手数 | 候補(圧縮) 平均/最大 | 候補(全列挙) 平均 | 自動進行 | " +
        (2..maxDepth).joinToString(" | ") { "${it}手先まで 平均/最大" })
    for ((name, a) in byStep.entries.map { it.key to it.value } + listOf("全体" to all)) {
        println("$name | ${a.n} | ${avg(a.opts, a.n)} / ${a.maxOpts} | ${avg(a.full, a.n)} | ${pct(a.forced.toDouble() / maxOf(1, a.n))} | " +
            (2..maxDepth).joinToString(" | ") { "${avg(a.la[it], a.n)} / ${a.laMax[it]}" })
    }
    println("候補数の分布 (圧縮): " + optHist.mapIndexed { i, c ->
        (if (i < 10) "${i + 1}" else if (i == 10) "11-20" else "21+") + ":$c" }.joinToString(" "))
    println("1手先読みの所要時間: 自動進行以外の1手あたり平均 ${fmt(all.nanos / 1e6 / maxOf(1, all.n - all.forced), 3)} ms、" +
        "評価1局面あたり約 ${fmt(evalNanos / 1e3, 1)} µs (盤面複製・特徴量抽出を含む)")
    for (d in 2..maxDepth) {
        val perMove = all.la[d].toDouble() / maxOf(1, all.n)
        println("${d}手先まで読む場合の見積り: 1手あたり評価 ${fmt(perMove, 1)} 局面 → 約 ${fmt(perMove * evalNanos / 1e6, 2)} ms/手")
    }
}

/**
 * ミニマックスの深さごとの思考時間の計測。1手先読みの対局から選択の余地がある局面を [every] 手おきに集め、
 * 同じ局面群に対して深さ 1..maxDepth で手を選ばせて、1手あたりの時間・評価局面数と、
 * 深さ1 / 1つ浅い深さ との手の一致率を出す。各深さは [budget] 秒で打ち切る (それまでに測れた局面で集計)。
 */
fun depthBench(o: Opts) {
    val wname = o.str("--weights") ?: "init_h1-best.gru.weight"
    val net = loadOrDie(wname)
    val maxDepth = o.int("--max-depth", 4)
    val games = o.int("--games", 4)
    val every = o.int("--every", 10)
    val budget = o.dbl("--budget", 120.0)
    val gamesPerIter = o.int("--games-per-iter", 16)
    // 計測する幅 (カンマ区切り、0 = 制限なし)。--width 1つだけの指定も可 (既定1)
    val widths = (o.str("--widths") ?: o.str("--width") ?: "1").split(',').mapNotNull { it.trim().toIntOrNull() }.ifEmpty { listOf(1) }
    val cfg = gameCfg(o)
    val rng = Random(o.long("--seed", 1))

    // 局面の収集
    val gen = NetAgent(net, "gen", temperature = 0.05, tempTurns = 4)
    val positions = ArrayList<GameState>()
    var decisions = 0L
    var moves = 0L
    repeat(games) {
        val state = createInitialGameState(cfg.deck1, cfg.deck2, rng.nextLong(), cfg.format)
        state.visitedPositions.add(positionHash(state))
        var step = 0
        while (state.winner == null && step < cfg.maxSteps) {
            if (optionsOf(state).size > 1) {
                if (decisions % every == 0L) positions.add(state.snapshot().also { it.visitedPositions = state.visitedPositions.toMutableSet() })
                decisions++
            }
            val a = gen.choose(state, rng).action
            val msgs = mutableListOf<String>()
            if (a == null) { advanceStep(state, msgs); state.visitedPositions.add(positionHash(state)) }
            else applyActionAndRecord(state, a, msgs)
            step++
        }
        moves += step
    }
    val decPerGame = decisions.toDouble() / games
    println("=== ミニマックス深さの計測: モデル $wname / 局面 ${positions.size} 件 (${games}局から${every}手おき) ===")
    println("1局あたり 手数 ${fmt(moves.toDouble() / games, 1)} / 選択の余地がある手 ${fmt(decPerGame, 1)}")
    println("幅 | 深さ | 測定局面 | 平均ms/手 | 最大ms | 平均評価局面 | 最大評価局面 | 深さ1と一致 | 1つ浅い深さと一致 | 同じ深さの幅制限なしと一致 | 1局の見積り | 1反復(${gamesPerIter}局)の見積り")

    val unlimited = HashMap<Int, List<String>>() // 深さ → 幅制限なしの選択
    for (w in widths) {
    var prev: List<String>? = null
    var first: List<String>? = null
    for (d in 1..maxDepth) {
        val agent = NetAgent(net, "d$d", depth = d, width = w)
        val picks = ArrayList<String>()
        var totalNs = 0L; var maxNs = 0L; var maxEval = 0L; var evalSum = 0L
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        for (p in positions) {
            if (start.elapsedNow().inWholeMilliseconds > budget * 1000) break
            val before = agent.evalCount
            val mark = kotlin.time.TimeSource.Monotonic.markNow()
            val c = agent.choose(p, Random(0))
            val ns = mark.elapsedNow().inWholeNanoseconds
            val ev = agent.evalCount - before
            totalNs += ns; maxNs = maxOf(maxNs, ns); evalSum += ev; maxEval = maxOf(maxEval, ev)
            picks.add(c.action?.detail ?: "ステップ終了")
        }
        val n = picks.size
        fun agree(ref: List<String>?) = if (ref == null) "-" else {
            val m = minOf(n, ref.size); pct((0 until m).count { picks[it] == ref[it] }.toDouble() / maxOf(1, m))
        }
        val avgMs = totalNs / 1e6 / maxOf(1, n)
        val perGameSec = avgMs * decPerGame / 1000
        if (w <= 0) unlimited[d] = picks
        println("${if (w > 0) w.toString() else "制限なし"} | $d | $n${if (n < positions.size) " (打切)" else ""} | ${fmt(avgMs, 2)} | ${fmt(maxNs / 1e6, 1)} | " +
            "${fmt(evalSum.toDouble() / maxOf(1, n), 1)} | $maxEval | ${agree(first)} | ${agree(prev)} | " +
            "${if (w > 0) agree(unlimited[d]) else "-"} | ${fmt(perGameSec, 1)}秒 | ${fmt(perGameSec * gamesPerIter / 60, 1)}分")
        if (d == 1) first = picks
        prev = picks
    }
    }
}

// ===================== セルフテスト =====================

fun selfTest() {
    var ok = true
    fun check(name: String, cond: Boolean, info: String) {
        println((if (cond) "[OK]   " else "[FAIL] ") + "$name: $info")
        if (!cond) ok = false
    }
    val rng = Random(7)
    fun randFeatures(len: Int) = Features(
        List(len) { DoubleArray(TOKEN_DIM) { if (rng.nextDouble() < 0.4) 0.0 else rng.nextDouble() * 2 - 1 } },
        DoubleArray(GLOBAL_DIM) { rng.nextDouble() * 2 - 1 },
        IntArray(len) { rng.nextInt(1_000_000) }
    )

    // 1. 勾配検査 (中心差分、カード番号の埋め込みを含む全パラメータ)
    val net = GruValueNet(hidden = 6, dense = 5, embBuckets = 7, embDim = 3).also { it.initRandom(Random(3)) }
    for (i in net.w.indices) net.w[i] *= 3.0 // 出力層の小初期化で勾配が消えないよう拡大
    val f = randFeatures(5)
    val target = 0.3
    val grad = DoubleArray(net.paramCount)
    net.accumulateGradient(f, target, grad)
    var maxRel = 0.0
    val eps = 1e-6
    for (i in net.w.indices) {
        val orig = net.w[i]
        net.w[i] = orig + eps; val lp = 0.5 * (net.evaluate(f) - target).let { it * it }
        net.w[i] = orig - eps; val lm = 0.5 * (net.evaluate(f) - target).let { it * it }
        net.w[i] = orig
        val num = (lp - lm) / (2 * eps)
        val rel = abs(num - grad[i]) / max(1e-7, abs(num) + abs(grad[i]))
        if (abs(num) + abs(grad[i]) > 1e-9) maxRel = max(maxRel, rel)
    }
    check("BPTT勾配検査", maxRel < 1e-4, "全${net.paramCount}パラメータ 最大相対誤差=$maxRel")

    // 2. 接頭辞キャッシュ付きバッチ評価 == 単体評価
    val big = GruValueNet.random(5)
    val base = randFeatures(6)
    val batch = List(8) { k ->
        val tail = randFeatures(k % 3)
        val n = 3 + k % 4
        Features(base.tokens.take(n) + tail.tokens, base.globals, base.cardIds.take(n).toIntArray() + tail.cardIds)
    }
    val bv = big.evaluateBatch(batch)
    val diff = batch.indices.maxOf { abs(bv[it] - big.evaluate(batch[it])) }
    check("バッチ評価一致", diff < 1e-12, "最大差=$diff")

    // 3. 学習で目標値へ収束するか (Adam)
    val small = GruValueNet.random(11)
    val data = List(16) { randFeatures(2 + it % 10) to (if (it % 2 == 0) 0.8 else -0.6) }
    val adam = Adam(small.paramCount, 0.01)
    var loss = 0.0
    repeat(400) {
        val g = DoubleArray(small.paramCount)
        loss = 0.0
        for ((x, y) in data) loss += small.accumulateGradient(x, y, g)
        for (i in g.indices) g[i] /= data.size
        adam.step(small.w, g)
        loss /= data.size
    }
    check("過学習テスト", loss < 0.01, "400step後の平均損失=${fmt(loss, 6)}")

    // 4. 保存・読込
    mkdirWeights()
    val path = "$WEIGHTS_DIR/_selftest.gru.weight"
    small.save(path)
    val back = GruValueNet.load(path)
    check("保存読込", back != null && back.w.contentEquals(small.w), "${small.paramCount}パラメータ")
    platform.posix.remove(path)

    // 4b. カード番号が評価に効くこと (能力値が同じでも番号が違えば V が変わる)
    run {
        val e = GruValueNet.random(21)
        val a = randFeatures(5)
        val b = Features(a.tokens, a.globals, IntArray(5) { a.cardIds[it] + 1 })
        val d = abs(e.evaluate(a) - e.evaluate(b))
        check("カード番号の入力", d > 1e-9, "番号だけ変えたときの V の差=$d")
    }

    // 4c. 埋め込みなしの旧形式 (version 1) の重みを読み込めること: 読み込み直後はカード番号に依存しない
    run {
        val old = GruValueNet.load("$WEIGHTS_DIR/g1_g2-best.gru.weight")
        if (old == null) {
            println("[SKIP] 旧形式の読込: weights/g1_g2-best.gru.weight が無い")
        } else {
            val st = createInitialGameState(seed = 5)
            val a = extractFeatures(st, 1)
            val b = Features(a.tokens, a.globals, IntArray(a.cardIds.size) { it * 7919 })
            val d = abs(old.evaluate(a) - old.evaluate(b))
            check("旧形式の読込", d < 1e-15, "パラメータ数=${old.paramCount} 番号を変えたときの V の差=$d")
        }
    }

    // 5. 実対局 (未学習ネット vs ランダム / 軌跡記録)
    val cfg = GameConfig(maxSteps = 300)
    val r = playGame(mapOf(1 to NetAgent(GruValueNet.random(1)), 2 to RandomAgent()), 42, cfg, Random(1), record = true)
    check("対局", r.steps > 0, "勝者=${r.winner ?: "引分"} ${r.turns}ターン ${r.steps}手 軌跡=${r.trajectories[1]!!.size}/${r.trajectories[2]!!.size}")

    // 5b. 圧縮列挙: 実対局の各局面で全列挙との候補数を比較し、圧縮側の手がすべて適用可能か確認
    var fullSum = 0L; var compactSum = 0L; var positions = 0; var broken = 0; var heuristic = 0
    var allSum = 0L; var brokenAll = 0; var allPositions = 0
    var levelSum = 0L; var brokenLevel = 0
    run {
        val st = createInitialGameState(seed = 3)
        val ag = RandomAgent()
        val rr = Random(3)
        var n = 0
        while (st.winner == null && n < 200) {
            if (enumerateActions(st).any { it.eval != 0.0 }) heuristic++ // エンジンが評価値を付けていないこと
            if (st.step == Step.MAIN || st.step == Step.MAIN_2) {
                val full = enumerateActions(st).count { !it.repeatsPosition }
                val comp = enumerateCompactActions(st).filterNot { it.repeatsPosition }
                fullSum += full; compactSum += comp.size; positions++
                for (a in comp) {
                    val before = positionHash(st)
                    val t = afterstate(st, a)
                    if (positionHash(t) == before) broken++ // 適用しても何も起きない手 = 不正な手
                }
                // 全合法手 (一括再配置を含む) は場所が多いと件数が爆発するので、場所5以下の局面だけ確かめる
                if (st.choosingPlayer.field.size <= 4) {
                    val allActs = enumerateAllActions(st).filterNot { it.repeatsPosition }
                    allSum += allActs.size; allPositions++
                    for (a in allActs) {
                        val before = positionHash(st)
                        if (positionHash(afterstate(st, a)) == before) brokenAll++
                    }
                }
                val lvActs = enumerateLevelActions(st).filterNot { it.repeatsPosition }
                levelSum += lvActs.size
                for (a in lvActs) {
                    val before = positionHash(st)
                    if (positionHash(afterstate(st, a)) == before) brokenLevel++
                }
            }
            val c = ag.choose(st, rr)
            val m = mutableListOf<String>()
            val act = c.action
            if (act == null) advanceStep(st, m) else applyActionAndRecord(st, act, m)
            n++
        }
    }
    check("エンジン評価値なし", heuristic == 0, "eval≠0 の列挙を含む局面=$heuristic")
    check("圧縮列挙", broken == 0 && positions > 0,
        "メイン局面$positions 件 平均候補数 全列挙=${fmt(fullSum.toDouble() / max(1, positions), 1)} → 圧縮=${fmt(compactSum.toDouble() / max(1, positions), 1)} / 無効手=$broken")
    check("全合法手の列挙", brokenAll == 0 && allSum > 0,
        "メイン局面$allPositions 件 (場所5以下) 平均候補数 全合法手=${fmt(allSum.toDouble() / max(1, allPositions), 1)} / 無効手=$brokenAll")
    check("Lv単位の列挙", brokenLevel == 0 && levelSum > 0,
        "メイン局面$positions 件 平均候補数 Lv単位=${fmt(levelSum.toDouble() / max(1, positions), 1)} (従来の全列挙=${fmt(fullSum.toDouble() / max(1, positions), 1)}) / 無効手=$brokenLevel")
    // 5b1. Lv単位の列挙では、プレイ等の間に続けて指せるコアの手 (Lv変更・ソウルコア移動) は「カード数+1」回まで
    run {
        val st = createInitialGameState(seed = 6)
        val rr = Random(6)
        var run = 0; var maxRun = 0; var overflow = 0; var n = 0
        var fieldAtRunStart = 0 // 連続の上限は「連続を始めた時点のカード数+1」(Lv変更で消滅したカードも1回と数える)
        while (st.winner == null && n < 400) {
            val opts = enumerateLevelActions(st).filterNot { it.repeatsPosition || it.forbidden }
            // コアの手を優先してランダムに選ぶ (循環が起きるならここで現れる)
            val core = opts.filter { it.rearrangeKey != null }
            val a: GameAction? = if (core.isNotEmpty() && rr.nextDouble() < 0.9) core[rr.nextInt(core.size)]
                else if (opts.isNotEmpty() && rr.nextBoolean()) opts[rr.nextInt(opts.size)] else null
            val m = mutableListOf<String>()
            if (a == null) {
                if (canEndStep(st) || opts.isEmpty()) advanceStep(st, m) else applyActionAndRecord(st, opts[0], m)
                run = 0
            } else {
                if (a.rearrangeKey != null) {
                    if (run == 0) fieldAtRunStart = st.choosingPlayer.field.size
                    run++
                } else run = 0
                maxRun = maxOf(maxRun, run)
                if (run > fieldAtRunStart + 1) overflow++
                applyActionAndRecord(st, a, m)
            }
            n++
        }
        check("Lv単位の列挙の循環なし", overflow == 0 && maxRun > 0, "${n}手 コアの手の最大連続=$maxRun / 上限超え=$overflow")
    }

    // 5b2. コアの一括再配置: 件数 = N×C(X+N−2, N−1) − 1 (ソウルコアなしは C(X+N−1, N−1) − 1)、
    //      適用後の配置が targetConfig と一致、直後は再配置を列挙しない
    run {
        fun binom(n: Int, k: Int): Long { if (k < 0 || k > n) return 0; var r = 1L; for (i in 1..k) r = r * (n - k + i) / i; return r }
        val st = createInitialGameState(seed = 4)
        val ag = RandomAgent()
        val rr = Random(4)
        var checked = 0; var countNg = 0; var configNg = 0; var afterNg = 0; var maxCount = 0
        var n = 0
        while (st.winner == null && n < 300 && checked < 15) {
            if ((st.step == Step.MAIN || st.step == Step.MAIN_2) && "ALL" !in st.coreWindow && st.choosingPlayer.field.isNotEmpty() && n % 3 == 0) {
                val p = st.choosingPlayer
                val places = 1 + p.field.size
                val normal = p.reserve.normal + p.field.sumOf { it.cores.normal }
                val soul = p.reserve.soul + p.field.sumOf { it.cores.soul }
                val expected = (if (soul > 0) places * binom(normal + places - 1, places - 1) else binom(normal + places - 1, places - 1)) - 1
                val acts = enumerateRearrangements(st)
                if (acts.size.toLong() != expected) countNg++
                maxCount = maxOf(maxCount, acts.size)
                for (a in acts.take(40)) {
                    val t = afterstate(st, a)
                    val tp = t.choosingPlayer
                    val ok = a.targetConfig.all { c ->
                        val (nn, ss) = if (c.sourceId == "Reserve") tp.reserve.normal to tp.reserve.soul
                            else tp.field.find { it.instanceId == c.sourceId }?.let { it.cores.normal to it.cores.soul } ?: (0 to 0)
                        // 消滅したカードのコアはリザーブへ戻るので、残っているカードだけ比べる
                        (c.sourceId != "Reserve" && tp.field.none { it.instanceId == c.sourceId }) || (nn == c.normal && ss == c.soul) || c.sourceId == "Reserve"
                    }
                    if (!ok) configNg++
                    if (enumerateRearrangements(t).isNotEmpty()) afterNg++
                }
                checked++
            }
            val c = ag.choose(st, rr)
            val m = mutableListOf<String>()
            val act = c.action
            if (act == null) advanceStep(st, m) else applyActionAndRecord(st, act, m)
            n++
        }
        check("コアの一括再配置", checked > 0 && countNg == 0 && configNg == 0 && afterNg == 0,
            "局面$checked 件 (最大${maxCount}通り) 件数と公式の不一致=$countNg / 配置の不一致=$configNg / 直後に再配置を列挙=$afterNg")
    }

    // 5b3. 並列実行: スレッド数によらず同じ対戦結果になること (局ごとに乱数の種を事前に決めているため)
    run {
        val net1 = GruValueNet.random(31)
        val a = NetAgent(net1, "a", temperature = 0.05, tempTurns = 4)
        val b = RandomAgent()
        val cfg6 = GameConfig(maxSteps = 120)
        val saved = Parallel.threads
        Parallel.threads = 1
        val s1 = arena(a, b, 6, 77, cfg6)
        Parallel.threads = 4
        val s4 = arena(a, b, 6, 77, cfg6)
        Parallel.threads = saved
        val same = s1.aWins == s4.aWins && s1.bWins == s4.bWins && s1.draws == s4.draws && s1.stepSum == s4.stepSum
        check("並列実行の再現性", same, "1スレッド ${s1.aWins}勝${s1.bWins}敗${s1.draws}分 ${s1.stepSum}手 / 4スレッド ${s4.aWins}勝${s4.bWins}敗${s4.draws}分 ${s4.stepSum}手")
    }

    // 5c. Elo 推定: Elo 0 の相手に 7勝2敗1分 (+仮想0.5勝/1局) → 400·log10(8/3)
    val elo = League.estimateElo(listOf(Triple(0.0, 7.5, 10)))
    val eloExpected = 400 * kotlin.math.log10(8.0 / 3.0)
    check("Elo推定", abs(elo - eloExpected) < 0.5, "推定=${fmt(elo, 1)} 理論値=${fmt(eloExpected, 1)}")

    // 5d. 探索の幅: 幅が候補数以上なら制限なしと同じ値、幅 k なら選ぶ手は1手先読みの上位 k 手のどれか
    run {
        val wnet = GruValueNet.random(9)
        val st = createInitialGameState(seed = 11)
        val ag = RandomAgent()
        val rr = Random(11)
        var checked = 0; var diffMax = 0.0; var outside = 0
        var n = 0
        while (st.winner == null && n < 120 && checked < 12) {
            val opts = optionsOf(st)
            if (opts.size >= 4 && n % 5 == 0) {
                val full = NetAgent(wnet, depth = 2, width = 0).evaluateOptions(st, opts)
                val wide = NetAgent(wnet, depth = 2, width = 1_000_000).evaluateOptions(st, opts)
                diffMax = max(diffMax, opts.indices.maxOf { abs(full[it] - wide[it]) })
                val shallow = NetAgent(wnet, depth = 1).evaluateOptions(st, opts)
                val top3 = opts.indices.sortedByDescending { shallow[it] }.take(3).toSet()
                val v3 = NetAgent(wnet, depth = 2, width = 3).evaluateOptions(st, opts)
                if (opts.indices.maxBy { v3[it] } !in top3) outside++
                checked++
            }
            val c = ag.choose(st, rr)
            val m = mutableListOf<String>()
            val act = c.action
            if (act == null) advanceStep(st, m) else applyActionAndRecord(st, act, m)
            n++
        }
        check("探索の幅", checked > 0 && diffMax < 1e-12 && outside == 0,
            "局面$checked 件 幅1000000(候補数以上)と制限なしの最大差=$diffMax / 幅3で上位3手の外を選んだ数=$outside")
    }

    // 6. λ-return
    val lr = lambdaReturns(doubleArrayOf(0.0, 0.5, 0.2), 1.0, 0.5)
    check("λ-return", abs(lr[2] - 1.0) < 1e-12 && abs(lr[1] - 0.6) < 1e-12 && abs(lr[0] - 0.55) < 1e-12, lr.joinToString())
    // 1手の報酬 −0.1: g2 = 1−0.1 = 0.9 / g1 = −0.1 + 0.5·0.2 + 0.5·0.9 = 0.45 / g0 = −0.1 + 0.5·0.5 + 0.5·0.45 = 0.375
    val lp = lambdaReturns(doubleArrayOf(0.0, 0.5, 0.2), 1.0, 0.5, 0.1)
    check("λ-return(1手の報酬)", abs(lp[2] - 0.9) < 1e-12 && abs(lp[1] - 0.45) < 1e-12 && abs(lp[0] - 0.375) < 1e-12, lp.joinToString())

    println(if (ok) "=== selftest: 全項目OK ===" else "=== selftest: 失敗あり ===")
}
