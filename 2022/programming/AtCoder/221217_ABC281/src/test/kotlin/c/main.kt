@file:Suppress("unused")

package c

import org.junit.jupiter.api.Test
import stdioEmulators
import testEnvBuilder

val testEnvs = listOf(
    testEnvBuilder {
        intake.put(
            """3 600
180 240 120
"""
        )

        outlet.observe(
            """1 60"""
        )
    }, testEnvBuilder {
        intake.put(
            """3 281
94 94 94
"""
        )

        outlet.observe(
            """3 93"""
        )
    }, testEnvBuilder {
        intake.put(
            """10 5678912340
1000000000 1000000000 1000000000 1000000000 1000000000 1000000000 1000000000 1000000000 1000000000 1000000000
"""
        )
        outlet.observe(
            """6 678912340"""
        )
    }
)

class test {
    @Test
    fun test() = stdioEmulators(testEnvs) { m() }
}

// -----------------------------------------------------------------------
//  main()
// -----------------------------------------------------------------------

fun main() = m()
fun m() {
    val (N, T) = rlvl
    val A = rlvl
    val r = A.sum()
    var r1 = T % r
    r1.err
    val i = (0..A.lastIndex).takeWhile {
        if (r1 < A[it]) {
             false
        } else {
            r1 -= A[it]
             true
        }
    }.count()+1
    println("$i $r1")
}


val <T> T.err get() = also { System.err.print("[$it]") }
val <T> T.errln get() = also { System.err.println("[$it]") }
val <T> T.pr get() = also { print(it) }
val <T> T.prln get() = also { println(it) }
fun <T : Comparable<T>> max(a: T, b: T) = if (a >= b) a else b
fun <T : Comparable<T>> min(a: T, b: T) = if (a < b) a else b
fun <T : Comparable<T>> max(vararg a: T) = a.max()!!
fun <T : Comparable<T>> min(vararg a: T) = a.min()!!

val rl get() = readLine()!!
val rli get() = rl.toInt()
val rll get() = rl.toLong()
val String.sp get() = split(" ")
val Iterable<String>.mapi get() = map { it.toInt() }
val Iterable<String>.mapl get() = map { it.toLong() }
val rlvi get() = rl.sp.mapi
val rlvl get() = rl.sp.mapl

fun Long.pow(l: Long) = (0L until l).fold(1L) { acc, _ -> acc * this }
fun Int.pow(i: Int) = (0 until i).fold(1) { acc, _ -> acc * this }
fun Long.digit(d: Long, base: Long = 10) = this / (base.pow(d)) % base
fun Int.digit(d: Int, base: Int = 10) = this / (base.pow(d)) % base
fun floor(v: Long, base: Long = 10) = v / base * base
fun round(v: Long, base: Long = 10) = floor(v + base / 2, base)
