package sample

import interop.Union1
import kotlinx.cinterop.*

fun unionSample() {
    val cUnion = cValue<Union1>()
    cUnion.useContents {
        i = 0x01020304
        println("cUnion.i=${i.toString(0x10)}")
        with(m) { listOf(uc3, uc2, uc1, uc0) }.joinToString { it.toString(0x10) }.let { println("cUnion.m=$it") }
        m.uc2 = 0xffu
        with(m) { listOf(uc3, uc2, uc1, uc0) }.joinToString { it.toString(0x10) }.let { println("cUnion.m=$it") }
        println("cUnion.i=${i.toString(0x10)}")
    }
}

