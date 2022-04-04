@file:Suppress("unused", "EXPERIMENTAL_IS_NOT_ENABLED")

package main.a

import testenv.stdioEmulatiors
import testenv.testEnv

val <T> T.err get() = also { System.err.print("[$it]") }
val <T> T.errln get() = also { System.err.println(it) }
fun <T : Comparable<T>> max(a: T, b: T) = if (a >= b) a else b
fun <T : Comparable<T>> min(a: T, b: T) = if (a < b) a else b
fun <T : Comparable<T>> max(vararg a: T) = a.max()!!
fun <T : Comparable<T>> min(vararg a: T) = a.min()!!

val rln get() = readLine()!!
val rlni get() = rln.toInt()
val String.sp get() = split(" ")
val Iterable<String>.mapi get() = map { it.toInt() }
val rlnvi get() = rln.sp.mapi

//val testEnvs = testEnv_AtCoder("ARC137", "C")
val testEnvsSample = listOf(testEnv {
    intake.println("INPUT")
    outlet.readLine() == "ANS"
})

val testEnvs = listOf(testEnv {
    intake.println("1 2")
    intake.println("2 2")
    intake.println("2 1")
    outlet.readLine() == "1 1"
})

fun main(): Unit = stdioEmulatiors(testEnvs) {
//    fun main(): Unit {

    val p = listOf(rlnvi, rlnvi, rlnvi)
    val px = p.map { it[0] }.groupBy { it }[1]!![0].errln
    val py = p.map { it[1] }.groupBy { it }[1]!![0].errln
    println("$px $py")
}


