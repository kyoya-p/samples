@file:Suppress("unused")

package d

import org.junit.jupiter.api.Test
import stdioEmulators
import testEnvBuilder
import java.util.*

val testEnvs = listOf(
    testEnvBuilder {
        intake.put(
            """6
2 7 1 8 2 8
"""
        )
        outlet.observe(
            """2
1
2
1
0
0
"""
        )
    },
    testEnvBuilder {
        intake.put(
            """10
979861204 57882493 979861204 447672230 644706927 710511029 763027379 710511029 447672230 136397527
"""
        )
        outlet.observe(
            """2
1
2
1
2
1
1
0
0
0
"""
        )
    }

)

class Test {
    @Test
    fun test() = stdioEmulators(testEnvs) { m() }
}

// -----------------------------------------------------------------------
//  main()
// -----------------------------------------------------------------------

// https://atcoder.jp/contests/abc273

fun main() = m()
fun m() {
    val N = rli
    val A = rlvl.groupingBy { it }.eachCount().toSortedMap()
    A.entries.reversed().forEach { println(it.value) }
    (A.size..N - 1).forEach { println(0) }
}

// Int(2^31) := 2.1 * 10^9
// UInt(2^32) := 4.2 * 10^9
// Long(2^63) := 9.2 * 10^18
// ULong(2^64) := 1.8 * 10^19

typealias FB = () -> Boolean
typealias FT<T> = () -> T

fun <T> ife(t1: FB, r1: FT<T>, e1: FT<T>) = if (t1()) r1() else e1()
fun <T> ife(t1: FB, r1: FT<T>, t2: FB, r2: FT<T>, e2: FT<T>) = ife(t1, r1, { ife(t2, r2, e2) })
fun <T> ife(t1: FB, r1: FT<T>, t2: FB, r2: FT<T>, t3: FB, r3: FT<T>, e3: FT<T>) =
    ife(t1, r1, t2, r2, { ife(t3, r3, e3) })

val <T> T.err get() = also { System.err.print("[$it]") }
fun <T> T.err(s: String) = also { System.err.print("[$s=$it]") }
val <T> T.errln get() = also { System.err.println("[$it]") }
fun <T> T.errln(s: String) = also { System.err.println("[$s=$it]") }
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

typealias BST = (Int) -> Boolean

fun bE2(s: Int, l: Int, m: Int = l / 2, t: BST) = when {
    l == 1 -> null
    l == 2 -> (s + 1) to 1
    t(s + m) -> s to (m + 1)
    else -> (s + m) to (m + 1)
}

fun bEdge(s: Int, l: Int, t: BST) = when {
    l == 0 -> s to 0
    l == 1 -> (if (t(s)) s else s + 1) to 1
    t(s) -> (s to 1)
    (!t(s + l - 1)) -> (s + l to 0)
    else -> generateSequence(s to l) { (s, l) -> bE2(s, l, t = t) }.last()
}

fun <T : Comparable<T>> List<T>.bEdgeBy(t: BST) = bEdge(0, size, t).first
fun <T : Comparable<T>> List<T>.bEdge(g: T) = bEdgeBy { get(it) >= g }
fun <T : Comparable<T>> MutableList<T>.insert(e: T) = add(bEdge(e), e)
fun <T : Comparable<T>> MutableList<T>.insert(v: Iterable<T>) = apply { v.forEach { e -> add(bEdge(e), e) } }
