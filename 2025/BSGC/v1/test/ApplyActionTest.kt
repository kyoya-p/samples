@file:Suppress("NonAsciiCharacters", "ObjectPropertyName")

import bssim.Action
import bssim.B
import bssim.Board
import bssim.Card
import bssim.FieldObject
import bssim.G
import bssim.Game
import bssim.P
import bssim.P1
import bssim.R
import bssim.R1
import bssim.Symbol
import bssim.Symbols
import bssim.W
import bssim.Y
import bssim.comb
import bssim.listChoices
import bssim.perm
import bssim.primaryColors
import kotlin.collections.listOf
import kotlin.test.Test
import kotlin.time.measureTime

fun <T> T.dbg(m: String = "") = apply { println("$m${this}") }


class WorldTest {
    @Test
    fun utils() {
        assert(perm(0, 0).toSet() == setOf(listOf<Int>()))
        assert(perm(1, 1).toSet() == setOf(listOf(0)))
        assert(perm(2, 2).toSet() == setOf(listOf(0, 1), listOf(1, 0)))
        assert(
            perm(3, 3).toSet() == setOf(
                listOf(0, 1, 2), listOf(0, 2, 1),
                listOf(1, 0, 2), listOf(1, 2, 0),
                listOf(2, 0, 1), listOf(2, 1, 0),
            )
        )
        assert(
            perm(3, 2).toSet() == setOf(
                listOf(0, 1), listOf(0, 2),
                listOf(1, 0), listOf(1, 2),
                listOf(2, 0), listOf(2, 1),
            )
        )
        assert(
            perm(4, 2).toSet() == setOf(
                listOf(0, 1), listOf(0, 2), listOf(0, 3),
                listOf(1, 0), listOf(1, 2), listOf(1, 3),
                listOf(2, 0), listOf(2, 1), listOf(2, 3),
                listOf(3, 0), listOf(3, 1), listOf(3, 2),
            )
        )

        assert(
            perm(4, 3).toSet() == setOf(
                listOf(0, 1, 2), listOf(0, 1, 3),
                listOf(0, 2, 1), listOf(0, 2, 3),
                listOf(0, 3, 1), listOf(0, 3, 2),

                listOf(1, 0, 2), listOf(1, 0, 3),
                listOf(1, 2, 0), listOf(1, 2, 3),
                listOf(1, 3, 0), listOf(1, 3, 2),

                listOf(2, 0, 1), listOf(2, 0, 3),
                listOf(2, 1, 0), listOf(2, 1, 3),
                listOf(2, 3, 0), listOf(2, 3, 1),

                listOf(3, 0, 1), listOf(3, 0, 2),
                listOf(3, 1, 0), listOf(3, 1, 2),
                listOf(3, 2, 0), listOf(3, 2, 1),
            )
        )

        assert(
            perm(4, 4).toSet() == setOf(
                listOf(0, 1, 2, 3), listOf(0, 1, 3, 2),
                listOf(0, 2, 1, 3), listOf(0, 2, 3, 1),
                listOf(0, 3, 1, 2), listOf(0, 3, 2, 1),

                listOf(1, 0, 2, 3), listOf(1, 0, 3, 2),
                listOf(1, 2, 0, 3), listOf(1, 2, 3, 0),
                listOf(1, 3, 0, 2), listOf(1, 3, 2, 0),

                listOf(2, 0, 1, 3), listOf(2, 0, 3, 1),
                listOf(2, 1, 0, 3), listOf(2, 1, 3, 0),
                listOf(2, 3, 0, 1), listOf(2, 3, 1, 0),

                listOf(3, 0, 1, 2), listOf(3, 0, 2, 1),
                listOf(3, 1, 0, 2), listOf(3, 1, 2, 0),
                listOf(3, 2, 0, 1), listOf(3, 2, 1, 0)
            )
        )

        assert(comb(0, 0).toList().dbg("C00=") == listOf(listOf<Int>()))
        assert(comb(0, 1).toList().dbg("C01=") == listOf<List<Int>>())
        assert(comb(1, 1).toList().dbg("C11=") == listOf(listOf(0)))
        assert(comb(2, 1).toList().dbg("C21=") == listOf(listOf(0), listOf(1)))
        assert(comb(2, 2).toList().dbg("C22=") == listOf(listOf(0, 1)))
        assert(comb(2, 3).toList().dbg("C23=") == listOf<List<Int>>())
        assert(comb(3, 0).toList().dbg("C30=") == listOf(listOf<Int>()))
        assert(comb(3, 1).toList().dbg("C31=") == listOf(listOf(0), listOf(1), listOf(2)))
        assert(comb(3, 2).toList().dbg("C32=") == listOf(listOf(0, 1), listOf(0, 2), listOf(1, 2)))
        assert(comb(3, 3).toList().dbg("C33=") == listOf(listOf(0, 1, 2)))
        assert(comb(3, 4).toList().dbg("C34=") == listOf<List<Int>>())
    }

    @Test
    fun utilsTime() {
        measureTime { perm(10, 10).count().dbg("perm(10):") }.dbg()
        measureTime { perm(11, 11).count().dbg("perm(11):") }.dbg()
        measureTime { perm(10, 10).count().dbg("comb(10):") }.dbg()
        measureTime { perm(11, 11).count().dbg("comb(11):") }.dbg()
    }

    @Test
    fun symbol() {

        val s1= Symbol(
            color = R,
            symbols = 1
        )
val ss1= Symbols

//        listOf('A', 'B').perm(2, 2).forEach {
//            println(it)
//        }

        //TODO
        //        val r1 = Symbol.R1
//        assert(r1.color == R)
//        assert(r1.color != G)
//
//        val g1 = Symbol.G1
//        assert(g1.color != R)
//        assert(g1.color == G)
//
//        val rg1 = union(R, G)
//        assert(rg1.isColor(R))
//        assert(rg1.isColor(G))
//        assert(!rg1.isColor(P))
//        assert(!rg1.isColor(C))
//        assert(C.isColor(C))
//        assert(!C.isColor(R))
    }

    @Test
    fun hands012() {
        val hand0 = listOf<Card.SpiritCard>()
        val hand1 = listOf(ドラグノ偵察兵)
        val hand2 = listOf(ドラグノ偵察兵, ゴラドン)

        val g0 = Game(Board(hand = hand0))
        val g1 = Game(Board(hand = hand1))
        val g2 = Game(Board(hand = hand2))

        assert(g0.listChoices().toSet() == setOf(Action.DoNothing))
        assert(g1.listChoices().toSet() == setOf(Action.DoNothing, Action.Summon(ドラグノ偵察兵)))
        assert(
            g2.listChoices().toSet() == setOf(
                Action.DoNothing,
                Action.Summon(ドラグノ偵察兵),
                Action.Summon(ゴラドン)
            )
        )
    }

    @Test
    fun field12() {
        val s1 = FieldObject.Spirit(ドラグノ偵察兵, 1)
        val g1 = Game(Board(hand = emptyList(), fieldObjects = listOf(s1)))
        assert(g1.listChoices().toSet() == setOf(Action.DoNothing, Action.SwapReserveCores(0)))

        val g2 = Game(Board(hand = emptyList(), fieldObjects = listOf(s1, s1)))
        assert(
            g2.listChoices().toSet() == setOf(
                Action.DoNothing,
                Action.SwapReserveCores(0),
                Action.SwapReserveCores(1),
                Action.SwapObjectCores(0, 1)
            )
        )
        val g3 = Game(Board(hand = emptyList(), fieldObjects = listOf(s1, s1, s1)))
        assert(
            g3.listChoices().toSet() == setOf(
                Action.DoNothing,
                Action.SwapReserveCores(0),
                Action.SwapReserveCores(1),
                Action.SwapReserveCores(2),
                Action.SwapObjectCores(0, 1),
                Action.SwapObjectCores(0, 2),
                Action.SwapObjectCores(1, 2),
            )
        )
    }

    @Test
    fun colors() {
        assert(primaryColors == setOf(R, P, G, W, Y, B))
        val e3 = listOf(1, 2, 3)
        val res0 = listOf(listOf<Int>())
        val res1 = e3.map { listOf(it) }
        val res2 = listOf(listOf(1, 2), listOf(1, 3), listOf(2, 1), listOf(2, 3), listOf(3, 1), listOf(3, 2))
        val res3 =
            listOf(listOf(1, 2, 3), listOf(1, 3, 2), listOf(2, 1, 3), listOf(2, 3, 1), listOf(3, 1, 2), listOf(3, 2, 1))

        assert(e3.perm(0).toSet() == res0.toSet())
        assert(e3.perm(1).toSet() == res1.toSet())
        assert(e3.perm(2).toSet() == res2.toSet())
        assert(e3.perm(3).toSet() == res3.toSet())

        val cRes0 = res0.map { it.toSet() }.toSet()
        val cRes1 = res1.map { it.toSet() }.toSet()
        val cRes2 = res2.map { it.toSet() }.toSet()
        val cRes3 = res3.map { it.toSet() }.toSet()

        assert(e3.comb(0).map { it.toSet() }.toSet() == cRes0.toSet())
        assert(e3.comb(1).map { it.toSet() }.toSet() == cRes1.toSet())

        println(e3.comb(2).map { it.toSet() }.toSet())
        println(cRes2)
        assert(e3.comb(2).map { it.toSet() }.toSet() == cRes2)

        println(e3.comb(3).map { it.toSet() }.toSet())
        println(cRes3)
        assert(e3.comb(3).map { it.toSet() }.toSet() == cRes3)
    }

    @Test

    fun fieldSymbols() {
//        fun Game.fieldSymbols(): Symbols = board.fieldObjects.fold(NoSymbol) { e, a -> a.symbols() + e }
//
//        val s1 = FieldObject.Spirit(ドラグノ偵察兵, 1)
//        val s2 = FieldObject.Spirit(`グリプ・ハンズ`, 1)
//
//        val G_1 = Game(Board())
//        val G_2 = Game(Board(fieldObjects = listOf(s1)))
//        val G_3 = Game(Board(fieldObjects = listOf(s1, s2, s1, s2)))
//
//        assert(G_1.fieldSymbols() == NoSymbol)
//        assert(G_2.fieldSymbols() == R1)
    }
}

val ゴラドン = Card.SpiritCard(
    name = "ゴラドン",
    cost = 0,
    reductionSymbol = 0,
    levelCosts = mapOf(1 to 1, 3 to 2),
    symbols = R1,
)
val ドラグノ偵察兵 = Card.SpiritCard(
    name = "ドラグノ偵察兵",
    cost = 2,
    reductionSymbol = 1,
    levelCosts = mapOf(1 to 1, 2 to 2),
    symbols = R1,
)

@Suppress("unused")
val メラット = Card.SpiritCard(
    name = "メラット",
    cost = 2,
    reductionSymbol = 2,
    levelCosts = mapOf(1 to 1, 2 to 3),
    symbols = R1,
)

@Suppress("unused")
val `グリプ・ハンズ` = Card.SpiritCard(
    name = "グリプ・ハンズ",
    cost = 3,
    reductionSymbol = 1,
    levelCosts = mapOf(1 to 1, 2 to 2),
    symbols = P1,
)
