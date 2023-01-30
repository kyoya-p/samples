@file:Suppress("unused")

package C

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

fun C() {
    val N = rli
    val A = rlvl
    val mapA = A.toSet().toList().sortedBy { -it }
    val counts = A.groupingBy { it }.eachCount()
    (0..N - 1).forEach { i ->
        println(mapA.getOrNull(i)?.let { counts[it]?.toLong() } ?: 0L)
    }
    //System.out.close()
}

fun main() = C()
