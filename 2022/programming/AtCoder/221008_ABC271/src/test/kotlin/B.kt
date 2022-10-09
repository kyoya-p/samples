package B

val <T> T.err get() = also { System.err.print("[$it]") }
val <T> T.errln get() = also { System.err.println(it) }
fun <T : Comparable<T>> max(a: T, b: T) = if (a >= b) a else b
fun <T : Comparable<T>> min(a: T, b: T) = if (a < b) a else b
fun <T : Comparable<T>> max(vararg a: T) = a.max()!!
fun <T : Comparable<T>> min(vararg a: T) = a.min()!!

val rl get() = readLine()!!
val rli get() = rl.toInt()
val String.sp get() = split(" ")
val Iterable<String>.mapi get() = map { it.toInt() }
val rlvi get() = rl.sp.mapi

fun B() {
    val (N, Q) = rlvi
    val a = (0 until N).map { rl }
    val t = (0 until Q).map { rlvi }
    fun aij(i: Int, j: Int) = a[i - 1].sp[j]
    (0 until Q).forEach { println(aij(t[it][0],t[it][1])) }
}

fun B1() {
    val (N, Q) = rlvi
    val a = (0 until N).map { rlvi }
    val t = (0 until Q).map { rlvi }
    (0 until Q).forEach { println(a[t[it][0] - 1][t[it][1] - 1]) }
}

fun main() = B()
