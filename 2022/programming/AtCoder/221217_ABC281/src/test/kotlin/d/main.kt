@file:Suppress("unused")

package d

import org.junit.jupiter.api.Test
import stdioEmulators
import testEnvBuilder

val testEnvs = listOf(
    testEnvBuilder {
        intake.put(
            """4 2 2
1 2 3 4
"""
        )

        outlet.observe(
            """6"""
        )
    }, testEnvBuilder {
        intake.put(
            """3 1 2
1 3 5
"""
        )

        outlet.observe(
            """-1"""
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
    val (N, K, D) = rlvl
    val A = rlvl.sortedDescending()
    val AD = A.groupBy { it % D }

//TODO

    println()
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
fun fact(n: Long) = (2..n).fold(1L) { a, e -> a * e }
fun perm(n: Long, r: Long) = (n - r + 1..n).fold(1L) { a, e -> a * e }
fun comb(n: Long, r: Long): Long = if (n / 2 < r) comb(n, n - r) else perm(n, r) / fact(r) //n個からr個選ぶ組合せ
fun hProd(n: Long, r: Long): Long = comb(n + r - 1, r)//homogeneous product n種からr個選ぶ組合せ
