@file:Suppress("unused")

package c

import org.junit.jupiter.api.Test
import stdioEmulators
import testEnvBuilder
import java.util.*

val testEnvs = listOf(
    testEnvBuilder {
        intake.put(
            """AB
"""
        )

        outlet.observe(
            """28
"""
        )
    }, testEnvBuilder {
        intake.put(
            """C
"""
        )

        outlet.observe(
            """3
"""
        )
    }, testEnvBuilder {
        intake.put(
            """BRUTMHYHIIZP
"""
        )

        outlet.observe(
            """10000000000000000
"""
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

// https://atcoder.jp/contests/abc285/tasks

fun main() = m()
fun m() {
    val S = rl.toList()
    val L = ('A'..'Z').toList()
    val id = S.fold(0L) { a, e -> a * 26 + L.indexOf(e) + 1 }
    println(id)
}

// Int(2^31) := 2.1 * 10^9
// UInt(2^32) := 4.2 * 10^9
// Long(2^63) := 9.2 * 10^18
// ULong(2^64) := 1.8 * 10^19

typealias FB = () -> Boolean
typealias FT<T> = () -> T

typealias F<T> = () -> T

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

fun bE1(s: Int, l: Int, m: Int = s + l / 2, t: BST) = if (t(m)) (s to (l / 2)) else (m to (s + l / 2 - m))
fun bE2(s: Int, l: Int, t: BST) = generateSequence(s to l) { (s, l) -> bE1(s, l, t = t).err }
fun bE3(s: Int, l: Int, t: BST) = bE2(s, l, t).dropWhile { (s, l) -> l > 0 }.first()
fun bEdge(s: Int, l: Int, t: BST) = when {
    l == 0 -> s to 0
    t(s) -> s to 1
    !t(s + l - 1) -> s + l to 0
    else -> bE3(s, l, t)
}

fun <T : Comparable<T>> List<T>.bEdge(g: T) = bEdge(0, size) { get(it) >= g }
fun <T : Comparable<T>> MutableList<T>.insert(e: T) = add(bEdge(e).first, e)
class ListWithOrigin<E>(val inner: List<E>, val origin: Int = 1) : List<E> by inner {
    override fun get(index: Int) = inner.get(index - origin)
    override fun listIterator(index: Int) = inner.listIterator(index - origin)
    override fun subList(fromIndex: Int, toIndex: Int) = inner.subList(fromIndex - origin, toIndex - origin)
}

fun <E> List<E>.origin1(origin: Int = 1) = ListWithOrigin(this, origin)
