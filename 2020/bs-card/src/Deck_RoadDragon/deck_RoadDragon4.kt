package BSSim.ロードラ4

import BSSim.*
import BSSim.ロードラ.*
import java.io.FileOutputStream
import java.io.PrintStream

val logger = PrintStream(FileOutputStream("log_roaddragon4.csv"))

fun main(args: Array<String>) {

    val deckBottom = DeckBottom()
    val deck = listOf(
            幼グラロロドラ(), 神世界トリスタ(), ダミー(), ちょうちん(), ちょうちん()
            , ダミー(), ダミー(), アプロディーテ()
            , ダミー()
            , ダミー(), ダミー(), ダミー(), ダミー(), 幼グラロロドラ()
            , ダミー(), ダミー(), ダミー()
            , ダミー(), ダミー(), チヒロ()
            , deckBottom
    )

    fun World.eval(): Int {
        return ownSide.deckDepth(deckBottom) * 100 - ownSide.fieldSimbols.toInt()
    }

    fun <T> Sequence<T>.cutBranchesBy(nTake: Int, nBest: Int, op: (T) -> Int): Sequence<T> = sortedBy { op(it) }.let { it.take(nBest) + it.drop(nBest).toList().shuffled() }.take(nTake)
    fun ParallelWorld.cutBranches(): ParallelWorld = cutBranchesBy(100, 10) { it.world.eval() }

    fun ParallelWorld.terminationCheck(termedWorlds: MutableSet<History>): ParallelWorld {
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
                    "${deck.joinToString("_")}, ${aStep}, ${nCond}, ${depth}, ${1.0 - nTerm * 1.0 / nCond}"
            )
        }
        return passStn
    }

    val termedWorld = mutableSetOf<History>()

    fun ParallelWorld.turn(): ParallelWorld = effect(eStartStep())
            .effect(eCoreStep(1))
            .effect(eDrawStep(1))
            .effect(eRefreshStep())
            .effect(eMainStep() { it.eval().toDouble() })

    History.eden(deck.toSet(), enemyDeck = setOf())
            .map_ownSide { mutation { reserve.core = Core(4, 0) } }
            .effect(eDrawStep(4)).distinctBy { it.world }.terminationCheck(termedWorld).cutBranches()
            .turn().terminationCheck(termedWorld).distinctBy { it.world }.cutBranches()
            .turn().terminationCheck(termedWorld).distinctBy { it.world }.cutBranches()
            .turn().terminationCheck(termedWorld).distinctBy { it.world }.cutBranches()
            //.turn().terminationCheck(termedWorld).distinctBy { it.world }.cutBranches()
            .forEachIndexed { i, it ->
//                it.pln()
//                println("$i:${it.world.eval()}  ${it.world}")
            }

    termedWorld.sortedBy { h ->
        h.world.step * 10000
        +h.world.ownSide.deckDepth(deckBottom) * 100
    }.sortedBy { it.world.step }.take(2).forEach {

        println("----------------------------------")
        println(it.world)
    }
}



