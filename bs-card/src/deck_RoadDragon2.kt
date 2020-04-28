package BSSim.ロードラ2

import BSSim.*
import BSSim.ロードラ.*
import kotlin.random.Random


fun main(args: Array<String>) {
    val seedTop = args[0]?.toInt()
    val loopCount = args[1]?.toInt()

    fun <T> x(n: Int, op: () -> T): List<T> = sequence { repeat(n) { yield(op()) } }.toList()
    val deckBase = x(3) { チヒロ() } + x(3) { キマリ() } + x(40) { ダミー() }
    val deckOpt = listOf(
            // x(7) { ダミー() } + x(8) { ダミー() }
            // x(3) { 幼グランロロ() } + x(3) { 神世界トリスタ() } + x(1) { 電人トレイン() } + x(8) { ダミー() }
            //x(3) { 幼グランロロ() } + x(2) { 神世界トリスタ() } + x(2) { 電人トレイン() } + x(8) { ダミー() }
            //, x(3) { 幼グランロロ() } + x(1) { 神世界トリスタ() } + x(3) { 電人トレイン() } + x(8) { ダミー() }

            x(3) { 幼グランロロ() } + x(3) { 神世界トリスタ() } + x(1) { 電人トレイン() } + x(8) { ダミー() } //エラーパターン (turn==7) ??
    )

    logger.println("cond, seed, step, nCase, eval, 1-termRatio, deck")

    for (deckOpt1 in deckOpt) {
        (seedTop until seedTop + loopCount).map { seed ->
            //thread(start = true) {
            test11(deckOpt1.toSet(), deckBase, seed)
            //}
        }.forEach { t ->
            //seet.join()
        }
    }
}

fun test11(deckOpt: Set<Card>, deckBase: List<Card>, seed: Int) {
    val deck = (deckOpt + deckBase).take(40)
    val deckBottom = DeckBottom()
    val deck1 = deck.shuffled(Random(seed)).toSet() + deckBottom

    fun World.eval(): Int {
        return ownSide.deckDepth(deckBottom) * 100 - ownSide.fieldSimbols.toInt()
    }

    fun <T> Sequence<T>.cutBranchesBy(nTake: Int, nBest: Int, op: (T) -> Int): Sequence<T> = sortedBy { op(it) }.let { it.take(nBest) + it.drop(nBest).toList().shuffled() }.take(nTake)
    fun ParallelWorld.cutBranches(): ParallelWorld = cutBranchesBy(20, 5) { it.world.eval() }

    fun ParallelWorld.terminationCheck(termedWorlds: MutableSet<History>): ParallelWorld {// 終了条件チェック
        val nCond = count()
        val passStn = flatMap { h ->
            if (h.world.ownSide.fieldObjectsMap.any { (_, a) -> a.cards[0] is チヒロ }
                    || h.world.ownSide.hand.cards.any { it is チヒロ }
            ) {
                termedWorlds.add(h)
                sequenceOf() //世界は継続しない
            } else {
                sequenceOf(h)
            }
        }
        val nTerm = nCond - passStn.count()
        val depth = sumBy { it.world.eval() } * 1.0 / count()
        if (nCond != 0) {
            val aStep = map { it.world.step }.average()
            logger.println(
                    "${deckOpt.joinToString("_")}, ${seed}, ${aStep}, ${nCond}, ${depth}, ${1.0 - nTerm * 1.0 / nCond},  ${deck1.joinToString("_")}"
            )
        }
        return passStn
    }

    println()
    print("seed=$seed ${deckOpt} ${deck1}")

    val termedWorld = mutableSetOf<History>()

    History.eden(deck1, enemyDeck = setOf())
            .map_ownSide { mutation { reserve.core = Core(4, 0) } }
            .effect(eDrawStep(4)).terminationCheck(termedWorld).cutBranches()
            .turn().terminationCheck(termedWorld).cutBranches()
            .turn().terminationCheck(termedWorld).cutBranches()
            .turn().terminationCheck(termedWorld).cutBranches()
            .turn().terminationCheck(termedWorld).cutBranches()
            .turn().terminationCheck(termedWorld).cutBranches()
            .turn().terminationCheck(termedWorld).cutBranches()
            .turn().terminationCheck(termedWorld).cutBranches()
            .forEachIndexed { i, it ->
                println("$i:${it.world.eval()}  ${it.world}")
            }


    //.turn().terminationCheck(termedWorld).cutBranches()
    //.turn().terminationCheck(termedWorld).cutBranches()
    //.turn().terminationCheck(termedWorld).cutBranches()
    //.turn().terminationCheck(termedWorld).cutBranches()

    termedWorld.sortedBy { h ->
        h.world.step * 1000
        +h.world.ownSide.deckDepth(deckBottom)
    }.take(5).forEach { println(it) }
}



