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
    val (N, K, D) = rlvi
    val A = rlvi

    val m = List(N + 1) { List(K + 1) { MutableList(D) { -1L } } }

    m[0][0][0] = 0
    for (n in 0 until N) {
        for (k in 0 until K + 1) {
            for (d in 0 until D) {
                if(m[n][k][d]==-1L)continue
                m[n + 1][k][d] = max(m[n + 1][k][d], m[n][k][d])  // A[i]を加算しないケース
                if (k != K) m[n + 1][k + 1][(d + A[n]) % D] = max(m[n + 1][k + 1][(d + A[n]) % D], m[n][k][d] + A[n])  // A[i]を加算するケース
            }
        }
    }

    println(m[N][K][0])
}

// Int(2^31) := 2.1 * 10^9
// UInt(2^32) := 4.2 * 10^9
// Long(2^63) := 9.2 * 10^18
// ULong(2^64) := 1.8 * 10^19

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

