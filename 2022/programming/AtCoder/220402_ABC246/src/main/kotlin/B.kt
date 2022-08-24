@file:Suppress("unused", "EXPERIMENTAL_IS_NOT_ENABLED")

package main.b

import testenv.stdioEmulatiors
import testenv.testEnv
import kotlin.math.sqrt

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
val Iterable<String>.mapf get() = map { it.toFloat() }
val rlnvi get() = rln.sp.mapi
val rlnvf get() = rln.sp.mapf


fun main(): Unit = stdioEmulatiors(testEnvs) {
//    fun main(): Unit {

    val (X, Y) = rlnvf
    val x2 = 1.0 / (1.0 + Y * Y / (X * X))
    val y2 = 1.0 - x2

    val x = sqrt(x2)
    val y = sqrt(y2)
    println("$x $y")
}



//val testEnvs = testEnv_AtCoder("ARC137", "C")
val testEnvsSample = listOf(testEnv {
    intake.println("INPUT")
    outlet.readLine() == "ANS"
})

val testEnvs = listOf(
    testEnv {
        intake.println("3 4")
        outlet.readLine() == "0.6 0.8"
    }
)