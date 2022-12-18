@file:Suppress("unused")

package b
import org.junit.jupiter.api.Test
import stdioEmulators
import testEnvBuilder


class test {
    @Test
    fun test() = stdioEmulators(testEnvs) { m() }
}
val testEnvs = listOf(
    testEnvBuilder {
        intake.put(
            """Q142857Z"""
        )

        outlet.observe(
            """Yes"""
        )
    }, testEnvBuilder {
        intake.put(
            """AB912278C"""
        )

        outlet.observe(
            """No"""
        )
    }, testEnvBuilder {
        intake.put(
            """X900000"""
        )

        outlet.observe(
            """No"""
        )
    }
)


// -----------------------------------------------------------------------
//  main()
// -----------------------------------------------------------------------

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
    fun f(s: String): Boolean {
        if (s.length > 10) return false
        val s1 = s.dropWhile { !it.isDigit() }
        if (s1.length == s.length) return false
        val s2 = s1.dropLastWhile { !it.isDigit() }
        if (s2.length == s1.length) return false
        if (s2.any { !it.isDigit() }) return false
        return true
    }
    when (f(rl)){
        true -> println("Yes")
        else -> println("No")
    }
}

fun main() = m()


