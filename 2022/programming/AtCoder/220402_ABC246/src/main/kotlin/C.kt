@file:Suppress("unused", "EXPERIMENTAL_IS_NOT_ENABLED")

package main.c

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
val Iterable<String>.mapf get() = map { it.toFloat() }
val rlnvi get() = rln.sp.mapi
val rlnvf get() = rln.sp.mapf

fun main(): Unit = stdioEmulatiors(testEnvs) {
//    fun main(): Unit {

    val (_, K, X) = rlnvi
    val A = rlnvi
    val Asyo = A.map { it / X }
    val Aama = A.map { it % X }.sortedDescending()
    val s = Asyo.sum()
    val r = Aama.drop(K - s).sum()
    println(r)
}



//val testEnvs = testEnv_AtCoder("ARC137", "C")
val testEnvsSample = listOf(testEnv {
    intake.println("INPUT")
    outlet.readLine() == "ANS"
})

val testEnvs = listOf(
    testEnv {
        intake.println("5 4 7")
        intake.println("8 3 10 5 13")
        outlet.readLine() == "12"
    }, testEnv {
        intake.println("5 100 7")
        intake.println("8 3 10 5 13")
        outlet.readLine() == "0"
    }
)


