package BSSim

import ロードラ.test

fun <T> T.pln(mark: String = "") = also { println("${mark}${this} ") }
fun <T> T.print() = also { print("$this ") }
fun <T> T.printh() = also { ("00" + hashCode().toUInt().toString(16)).let { it.substring(it.length - 3) }.print() }
fun <T> T.assert(test: T) = also {
    if (this == test) //println("T:${this}==${test}")
    else {
        pln("F:${this}!=${test}")
        throw Exception()
    }
}

fun Int.if0(op: () -> Int) = if (this == 0) op() else this

fun main() {
    Sbl.test()
    Core.test()
    BSO.test()
    Side_XXX_test()
    Situation.test()

    test()
}

fun Side_XXX_test() {
    val Ca1 = ちょうちん()
    val Ca1a = ちょうちん()
    val Ca2 = 子フ()
    val Ca3 = ピグレ()
    val Ca4 = オルリ()

    val FO1 = BSO("f1", cards = listOf(Ca1, Ca2), core = Core(1))
    val FO1x = BSO("f1", cards = listOf(Ca1, Ca2), core = Core(1))

    val FO2 = BSO("f2", cards = listOf(Ca4), core = Core(2))
    val FO3 = BSO("f3", cards = listOf(Ca1), core = Core(3))
    val FO4 = BSO("f4", cards = listOf(Ca3), core = Core(4))
    FO1.assert(FO1x)
    val side1 = Side_XXX(listOf(FO2, FO1, FO3, FO4))
    val side2 = Side_XXX(listOf(FO2, FO1x, FO3, FO4))

    side1.assert(side2)

    val deck2: BSOs = listOf(FO1, FO2, FO3, FO4)
    deck2.pickTop(2).take1Case().assert(listOf(FO1, FO2) to listOf(FO3, FO4))
    deck2.pickBottom(2).take1Case().assert(listOf(FO4, FO3) to listOf(FO1, FO2))

    val bso1 = BSO(id = "aaa", cards = listOf(Ca1), core = Core(1, 1))
    val bso2 = BSO(id = "aaa", cards = listOf(Ca1a), core = Core(1, 1))
    bso1.assert(bso2)

    val s1 = Side_XXX()
    val s2 = s1.tr()
    s2.bsObjects.assert(s1.bsObjects)
    s2.assert(s1)

}

