package main.c

import testenv.stdioEmulatiors
import testenv.testEnv_AtCoder

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

fun main(): Unit = stdioEmulatiors(testEnv_AtCoder("ARC137", "C")) {
//    fun main(): Unit {
    val N = rlni
    val A = rlnvi
    var (mx1, mx2) = listOf(0, 0)
    A.forEach { e ->
        when {
            e > mx2 && e < mx1 -> mx2 = e
            e > mx2 && e > mx1 -> {
                mx2 = mx1
                mx1 = e
            }
        }
    }

    //"$N $mx1 $mx2".errln
    if (mx1 - mx2 >= 2) {
        println("Alice")
    } else {
        if (N % 2 == mx1 % 2) println("Alice") else println("Bob")
    }
}


