@file:Suppress("unused")

package a

import org.junit.jupiter.api.Test
import stdioEmulators
import testEnvBuilder

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

fun m() {
    val N = rli
    for (i in N downTo 0) {
        println(i.err)
    }
}


fun main() = m()

val testEnvs = listOf(
    testEnvBuilder {
        intake.put(
            """3"""
        )

        outlet.observe(
            """3
2
1
0"""
        )
    }
)


class test {
    @Test
    fun test() = stdioEmulators(testEnvs) { m() }
}