package main.b

import testenv.stdioEmulatiors
import testenv.testEnv_AtCoder

import java.lang.Integer.max
import java.lang.Integer.min

val <T> T.err get() = also { System.err.print("[$it]") }
val <T> T.errln get() = also { System.err.println(it) }

val rln get() = readLine()!!
val rlni get() = rln.toInt()
val String.sp get() = split(" ")
val Iterable<String>.mapi get() = map { it.toInt() }


fun main(): Unit = stdioEmulatiors(testEnv_AtCoder("ARC137", "B")) {
    var (d01, dmin, dmax, amax, amin) = listOf(0, 0, 0, 0, 0)
    rlni
    rln.sp.mapi.forEach { e ->
        if (e == 0) --d01 else ++d01
        amin = min(amin, d01 - dmax)
        amax = max(amax, d01 - dmin)
        dmin = min(dmin, d01)
        dmax = max(dmax, d01)
        //"$e : $dmax, $dmin, $amax, $amin".errln
    }
    println(amax - amin + 1)
}


