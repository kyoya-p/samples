import bstools.model.*
import bstools.kann.kad_srand
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.random.Random

/**
 * KANN(cinterop連携したC言語のNNライブラリ)のGRU価値ネットワークを、進化戦略(ES)による
 * 自己対戦で強化学習する。
 *
 * `KannValueNetwork.trainStep`(kann_unroll_arrayを使ったBPTT)は繰り返し呼び出すと
 * 実測で約17〜19回目に確実にクラッシュする既知の不具合があり(KANN側`kad_unroll_helper`の
 * 実装依存の問題と見られ、cinterop越しにこちらから修正できない)、実用に耐えない。
 * このファイルは**その不具合のあるパスを一切使わず**、既に安定動作を確認済みの
 * [KannValueNetwork.evaluate](連続フィード方式、unrollを使わない)のみを使う。
 *
 * 設計は `Tuner.kt` の `tuneRnnWeights`/`evaluateCandidateRnn`(自作GRU版のES)と同じ
 * (1+1)型山登り法。対局は評価値greedy(自分の重みで最良の手を選ぶ)の自己対戦で行い、
 * 勝率を報酬とする — 「ランダムプレイでラベル付けしてBPTTで回帰する」設計とは異なり、
 * 重み自体が対局の意思決定に使われるため、ES(勝率を直接最適化)と自然に噛み合う。
 *
 * KANNの`ann`(kann_t*)はインスタンスごとに1つの計算グラフを共有するため、複数スレッドから
 * 同時に`evaluate`/`setWeights`すると競合してクラッシュしうる(cinterop連携は
 * `@ThreadLocal`のような仕組みを持たない)。安定性を優先し、あえて単一スレッドで実装する。
 */

// 1手先読み評価(evaluateActionWithKann)とステップ終了の基準値(kannStepEndEval)は
// model/src/KannRnnEvaluator.kt の共通実装を使う ([EvalMode.KANN] 経路と重複させない)。

/**
 * 「本当に進行しているか」を外部(GUIの`/api/training-status`)から見えるようにするための
 * 診断用カウンタ。世代・対局番号は[runKannEsTraining]/[evaluateCandidateKann]側で更新し、
 * 手数は[playoutGameKann]の1手ごとに増分する。単調増加が止まっていれば、対局が本当に
 * ハングしている(無限ループ)ことを疑える(逆に増え続けていれば「低速だが生きている」)。
 */
private object EsDiag {
    var generation = 0
    var totalGenerations = 0
    var games = 0
    var adoptedCount = 0
    var outPath = ""
    var gameIndex = 0
    var stepInGame = 0L
    var totalSteps = 0L
    /**
     * 1手ごとに何個の局面(候補行動+ステップ終了の評価値)をKANNへ評価させたかの累計。
     * `totalSteps`(手数)は「何手進んだか」しか表さないが、実際の計算コストは
     * 「1手あたり何個の候補を評価したか」に比例する。手数の増加が鈍っても評価局面数が
     * 順調に増えていれば「1手あたりの候補が多いだけで生きている」、逆に評価局面数まで
     * 止まっていれば本当のハングだと判断できる。
     */
    var evaluatedPositions = 0L
    /**
     * プロセス起動から通算で遭遇した**重複を除いた**局面ハッシュの集合。対局ごとにリセットする
     * [GameState.visitedPositions](同一局面への手を弾くためだけの、対局内限定の集合)とは別物。
     * こちらはプロセス全体を通じて保持し続け、サイズ(size)だけを指標として公開する。
     * 手数(totalSteps)や評価局面数(evaluatedPositions)が増えていても、実は同じ局面を
     * 行ったり来たりしているだけ(相殺されて盤面が実質進んでいない)なら、この値は
     * ほとんど増えない — 逆にこれが順調に増え続けていれば、本当に新しい局面を
     * 探索し続けていることの一番強い証拠になる。
     */
    val distinctPositionsSeen = HashSet<Long>()
}

private fun writeEsHeartbeat(winRate: Double? = null, adopted: Boolean? = null) {
    writeEsTrainingProgress(
        EsTrainingProgress(
            active = true,
            generation = EsDiag.generation,
            totalGenerations = EsDiag.totalGenerations,
            games = EsDiag.games,
            winRate = winRate,
            adopted = adopted,
            adoptedCount = EsDiag.adoptedCount,
            outPath = EsDiag.outPath,
            updatedAtMs = currentTimeMs(),
            gameIndex = EsDiag.gameIndex,
            stepInGame = EsDiag.stepInGame,
            totalSteps = EsDiag.totalSteps,
            evaluatedPositions = EsDiag.evaluatedPositions,
            distinctPositionsSeen = EsDiag.distinctPositionsSeen.size.toLong()
        )
    )
}

/**
 * 手番ごとにnetの重みを差し替えながら1局最後まで自己対戦させ、勝者(1 or 2)を返す。
 * 手数上限に達しても決着しない場合は null (引き分け扱い)。
 */
private fun playoutGameKann(net: KannValueNetwork, weightsByPlayer: Map<Int, FloatArray>, seed: Long, maxSteps: Int): Int? {
    val state = createInitialGameState(seed = seed)
    repeat(maxSteps) { step ->
        if (state.winner != null) return state.winner

        EsDiag.stepInGame = step.toLong()
        EsDiag.totalSteps++
        EsDiag.distinctPositionsSeen.add(positionHash(state))
        // 5手ごとに進捗を書き出す(毎手だとI/Oが増えすぎるため間引く)。stepInGame/totalSteps
        // が単調増加し続けているかどうかで、対局が本当に進行しているかを外部から診断できる。
        if (step % 5 == 0) writeEsHeartbeat()

        net.setWeights(weightsByPlayer[state.choosingPlayerId] ?: return@repeat)
        val actions = enumerateActions(state)
        val messages = mutableListOf<String>()
        // repeatsPosition(同一局面へ戻る手)はmarkRepetitions([model/src/Rules.kt])が
        // eval=-1.0にして選ばれにくくしているだけで、候補から除外はしていない。
        // KANNのtanh出力は極端に変異した個体だとfloat32で正確に-1.0へ飽和しうるため、
        // 「他のどの選択肢よりも低い」という前提が崩れると、過去にRNN評価器で実際に発生した
        // 「同一局面に戻る手を選び続けて300手ずっと停止する」無限ループ(Rules.ktのコメント参照)が
        // KANNでも再発しうる。評価値の大小に依存せず、ここで明示的に除外して確実に回避する。
        val candidates = actions.filterNot { it.forbidden || it.repeatsPosition }
        val scored = candidates.map { it to evaluateActionWithKann(state, it, net) }
        EsDiag.evaluatedPositions += candidates.size
        val best = scored.maxByOrNull { it.second }

        if (state.step == Step.BLOCK_DECLARATION) {
            if (best != null) applyActionAndRecord(state, best.first, messages)
        } else {
            val endEval = kannStepEndEval(state, net)
            EsDiag.evaluatedPositions++
            if (best != null && best.second > endEval) {
                applyActionAndRecord(state, best.first, messages)
            } else {
                advanceStep(state, messages)
                state.visitedPositions.add(positionHash(state))
            }
        }
    }
    return state.winner
}

/** candidate 側の勝率を baseline との自己対戦で測る。先攻/後攻を交互に入れ替えて先手有利を打ち消す */
private fun evaluateCandidateKann(net: KannValueNetwork, candidate: FloatArray, baseline: FloatArray, games: Int, rng: Random, maxSteps: Int): Double {
    var score = 0.0
    var draws = 0
    repeat(games) { i ->
        EsDiag.gameIndex = i + 1
        val seed = rng.nextLong()
        val candidateIsP1 = i % 2 == 0
        val weightsByPlayer = if (candidateIsP1) mapOf(1 to candidate, 2 to baseline) else mapOf(1 to baseline, 2 to candidate)
        val winner = playoutGameKann(net, weightsByPlayer, seed, maxSteps)
        if (winner == null) draws++
        score += when {
            winner == null -> 0.5
            candidateIsP1 && winner == 1 -> 1.0
            !candidateIsP1 && winner == 2 -> 1.0
            else -> 0.0
        }
    }
    if (draws > 0) println("  (診断: $games 局中 $draws 局が引き分け=決着せず)")
    return score / games
}

/**
 * 初期局面で「最良の行動」が「ステップ終了(END_STEP)」をどれだけ上回るかを測る。
 * 未学習の初期重みは高確率で END_STEP を常に他の全行動より高く評価してしまい
 * ([evaluateActionWithKann]で先読みした値がどれもEND_STEPに負ける)、
 * `best.second > kannStepEndEval(...)` ([playoutGameKann]) が常にFalseになって
 * 両者とも「常にパス」の固定方策に落ち、ESの重み変異が対局結果に反映されなくなる
 * (実測: sigma=0でも sigma=0.05でも毎世代ちょうど勝率50.0%に固着することを確認した)。
 * このマージンが正(いずれかの行動がEND_STEPを上回る)になる初期化を探すために使う。
 */
private fun endStepMargin(net: KannValueNetwork, state: GameState): Double {
    val actions = enumerateActions(state).filterNot { it.forbidden }
    if (actions.isEmpty()) return Double.NEGATIVE_INFINITY
    val bestV = actions.maxOf { evaluateActionWithKann(state, it, net) }
    return bestV - kannStepEndEval(state, net)
}

/**
 * KANNの重み初期化に使う大域RNG([kad_srand])のシードを何通りか試し、[endStepMargin]が
 * 最大(理想的には正)になる初期化を採用する。「ランダム初期重みで開始」というESの
 * 局所解対策として、学習前にこの探索を1回だけ行う。
 */
@OptIn(ExperimentalForeignApi::class)
private fun findGoodInitNet(trySeeds: Int, baseSeed: Long): KannValueNetwork {
    val state = createInitialGameState()
    var bestNet: KannValueNetwork? = null
    var bestMargin = Double.NEGATIVE_INFINITY
    var bestSeed = baseSeed
    for (i in 0 until trySeeds) {
        val seed = baseSeed + i
        kad_srand(null, seed.toULong())
        val net = KannValueNetwork()
        val margin = endStepMargin(net, state)
        if (margin > bestMargin) {
            bestMargin = margin
            bestSeed = seed
            bestNet?.delete()
            bestNet = net
        } else {
            net.delete()
        }
    }
    println("初期重み探索: $trySeeds 通り中 seed=$bestSeed を採用 (END_STEP優位マージン=${fmtMargin(bestMargin)})")
    return bestNet!!
}

/** 小数第4位までの文字列にする (Kotlin/Native には String.format が無いため手書き) */
private fun fmtMargin(v: Double): String {
    val scaled = (v * 10000).toInt()
    val sign = if (scaled < 0) "-" else ""
    val a = kotlin.math.abs(scaled)
    return "$sign${a / 10000}.${(a % 10000).toString().padStart(4, '0')}"
}

/** 重み配列全体に独立な小さいガウスノイズを同時に加える ([RnnParams.mutate] と同じ発想) */
private fun mutateWeights(w: FloatArray, rng: Random, sigma: Float): FloatArray = FloatArray(w.size) { i ->
    val u1 = rng.nextDouble().coerceAtLeast(1e-12)
    val u2 = rng.nextDouble()
    val gaussian = kotlin.math.sqrt(-2.0 * kotlin.math.ln(u1)) * kotlin.math.cos(2.0 * kotlin.math.PI * u2)
    w[i] + (gaussian * sigma).toFloat()
}

/** 0.0〜1.0 を小数第1位までの % 文字列にする (Kotlin/Native には String.format が無いため手書き) */
private fun pct(v: Double): String {
    val hundredths = (v * 1000).toInt()
    return "${hundredths / 10}.${hundredths % 10}%"
}

/** [KannValueNetwork.save]([kann_save]形式)のファイルから重み配列だけを取り出す。グラフ構造は使い捨てる */
private fun loadWeightsFromFile(path: String): FloatArray {
    val loaded = KannValueNetwork.load(path) ?: error("重みファイルを読み込めません: $path")
    val w = loaded.getWeights()
    loaded.delete()
    return w
}

/**
 * `--kann-es-train` 起動時のエントリポイント。
 * `--generations` `--games` `--sigma` `--seed` `--max-steps` `--out` `--init-search` `--resume` で調整できる。
 *
 * `--resume <path>` を指定しない場合、学習前に [findGoodInitNet] で初期重みを探索する。
 * 未学習の初期重みのままだと高確率でEND_STEPが他の全行動より常に高く評価され、両者とも
 * 「常にパス」の固定方策に陥って重みを変異させても対局結果が一切変わらなくなる
 * (実測: 全世代がちょうど勝率50.0%に固着し学習が進まなかった)。初期重み探索でこの局所解を
 * 避けてから学習を始める。
 *
 * `--resume <path>` を指定した場合、そのファイル([KannValueNetwork.save]形式)を初期チャンピオン
 * として続きから学習する([findGoodInitNet]は実行しない)。同じファイルを`--out`にも指定すれば
 * 上書き更新できる(例: `--resume kann-es-final.bin --out kann-es-final.bin`)。
 */
fun runKannEsTraining(args: Array<String>) {
    fun intArg(flag: String, default: Int): Int {
        val idx = args.indexOf(flag)
        return if (idx >= 0 && idx + 1 < args.size) args[idx + 1].toIntOrNull() ?: default else default
    }
    fun doubleArg(flag: String, default: Double): Double {
        val idx = args.indexOf(flag)
        return if (idx >= 0 && idx + 1 < args.size) args[idx + 1].toDoubleOrNull() ?: default else default
    }

    val generations = intArg("--generations", 20)
    val games = intArg("--games", 12)
    val sigma = doubleArg("--sigma", 0.05).toFloat()
    val seed = intArg("--seed", 1)
    // 実測: 手数上限80では自己対戦が決着せず全局引き分けになり、勝率が常に50.0%に
    // 固着して学習が進まなかった。300なら安定して決着することを確認済み ([Tuner.kt]の
    // 自作GRU版ESと同じデフォルト300に揃える)。
    val maxSteps = intArg("--max-steps", 300)
    val outIdx = args.indexOf("--out")
    val outPath = if (outIdx >= 0 && outIdx + 1 < args.size) args[outIdx + 1] else "kann-value-es.bin"
    val initSearch = intArg("--init-search", 30)
    // 既存の学習済みモデルを初期チャンピオンとして続きから学習する(ゼロから[findGoodInitNet]で
    // 探索し直さない)。ES自体は局所探索(山登り法)なので、良い出発点から続けるほうが
    // 同じ世代数でも早く強くなる。
    val resumeIdx = args.indexOf("--resume")
    val resumePath = if (resumeIdx >= 0 && resumeIdx + 1 < args.size) args[resumeIdx + 1] else null

    val rng = Random(seed)
    val net: KannValueNetwork
    var champion: FloatArray
    if (resumePath != null) {
        net = KannValueNetwork()
        champion = loadWeightsFromFile(resumePath)
        if (champion.size != net.nVar) error("重みファイルのパラメータ数(${champion.size})がネットワーク構成(${net.nVar})と一致しません: $resumePath")
        net.setWeights(champion)
    } else {
        net = findGoodInitNet(initSearch, seed.toLong() * 1000003L)
        champion = net.getWeights()
    }
    var adoptedCount = 0

    println("=== KANN価値ネットワーク: 進化戦略(ES)による自己対戦学習開始 (目的関数: 勝率) ===")
    println("パラメータ数=${net.nVar}, 世代数=$generations, 1世代あたりの対戦数=$games, 摂動幅=$sigma, 手数上限=$maxSteps")
    println(if (resumePath != null) "初期チャンピオン: $resumePath から続きを学習" else "初期チャンピオン: 新規探索(未学習)")
    println()

    EsDiag.totalGenerations = generations
    EsDiag.games = games
    EsDiag.outPath = outPath

    for (gen in 1..generations) {
        EsDiag.generation = gen
        EsDiag.adoptedCount = adoptedCount
        val candidate = mutateWeights(champion, rng, sigma)
        val winRate = evaluateCandidateKann(net, candidate, champion, games, rng, maxSteps)
        val adopted = winRate > 0.5
        println("世代$gen: 候補勝率=${pct(winRate)} ${if (adopted) "→ 採用" else "→ 棄却"}")
        if (adopted) {
            champion = candidate
            adoptedCount++
        }
        // GUIを提供している別プロセス(GameServer/Playmats)が /api/training-status 経由で
        // 参照できるよう、世代ごとに進捗をファイルへ書き出す(プロセス間はこれでしか繋がらない)。
        EsDiag.adoptedCount = adoptedCount
        writeEsHeartbeat(winRate = winRate, adopted = adopted)
    }

    println()
    println("=== 学習完了 ($adoptedCount / $generations 世代で採用) ===")
    net.setWeights(champion)
    net.save(outPath)
    writeEsTrainingProgress(
        EsTrainingProgress(
            active = false,
            generation = generations,
            totalGenerations = generations,
            games = games,
            winRate = null,
            adopted = null,
            adoptedCount = adoptedCount,
            outPath = outPath,
            updatedAtMs = currentTimeMs(),
            gameIndex = EsDiag.gameIndex,
            stepInGame = EsDiag.stepInGame,
            totalSteps = EsDiag.totalSteps,
            evaluatedPositions = EsDiag.evaluatedPositions,
            distinctPositionsSeen = EsDiag.distinctPositionsSeen.size.toLong()
        )
    )
    println("=== 学習済みモデルを保存: $outPath ===")
    net.delete()
}

/**
 * `--kann-match` 起動時のエントリポイント。player1/player2 それぞれに独立した重み
 * ([KannValueNetwork.save]形式のファイル、`--p1-weights`/`--p2-weights`)を指定して
 * 対戦させ、勝敗を集計する。[playoutGameKann]は元々プレイヤーごとに異なる重み
 * ([weightsByPlayer]) を扱える設計だったが、これまで `--kann-es-train` の内部
 * (champion/candidate、いずれも同じ初期化由来)でしか使われていなかった。
 * このモードはそれをCLIから任意の2つの重みで使えるようにする。
 *
 * どちらか(または両方)を省略した場合は [findGoodInitNet] で探索した未学習の初期重みで
 * 代用する。`--games` `--max-steps` `--seed` `--init-search` で調整できる。
 */
fun runKannMatch(args: Array<String>) {
    fun intArg(flag: String, default: Int): Int {
        val idx = args.indexOf(flag)
        return if (idx >= 0 && idx + 1 < args.size) args[idx + 1].toIntOrNull() ?: default else default
    }
    fun strArg(flag: String): String? {
        val idx = args.indexOf(flag)
        return if (idx >= 0 && idx + 1 < args.size) args[idx + 1] else null
    }

    val games = intArg("--games", 20)
    val maxSteps = intArg("--max-steps", 300)
    val seed = intArg("--seed", 1)
    val initSearch = intArg("--init-search", 30)
    val p1Path = strArg("--p1-weights")
    val p2Path = strArg("--p2-weights")

    val rng = Random(seed)
    // 対局の実行には土台となる1つの ann (グラフ構造) が要る。片方でも重みファイルが
    // 省略された場合に備え、常に探索済みの初期重みネットワークを用意しておく。
    val net = findGoodInitNet(initSearch, seed.toLong() * 1000003L)
    val w1 = p1Path?.let { loadWeightsFromFile(it) } ?: net.getWeights()
    val w2 = p2Path?.let { loadWeightsFromFile(it) } ?: net.getWeights()
    if ((p1Path != null || p2Path != null) && (w1.size != net.nVar || w2.size != net.nVar)) {
        error("重みファイルのパラメータ数(${w1.size}/${w2.size})がネットワーク構成(${net.nVar})と一致しません")
    }

    println("=== KANN価値ネットワーク: 対戦評価 ===")
    println("player1=${p1Path ?: "(未学習初期重み)"}, player2=${p2Path ?: "(未学習初期重み)"}, 対戦数=$games, 手数上限=$maxSteps")
    println()

    var wins1 = 0
    var wins2 = 0
    var draws = 0
    for (g in 1..games) {
        val gameSeed = rng.nextLong()
        val winner = playoutGameKann(net, mapOf(1 to w1, 2 to w2), gameSeed, maxSteps)
        when (winner) {
            1 -> wins1++
            2 -> wins2++
            else -> draws++
        }
        println("対戦$g/$games: winner=${winner?.let { "player$it" } ?: "引分"}")
    }

    println()
    println("=== 結果: player1=${pct(wins1.toDouble() / games)}(${wins1}勝) player2=${pct(wins2.toDouble() / games)}(${wins2}勝) 引分=${pct(draws.toDouble() / games)}(${draws}局) ===")
    net.delete()
}
