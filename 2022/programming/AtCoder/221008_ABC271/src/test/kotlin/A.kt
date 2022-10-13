package A

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

fun A() {
    val i = rli
    val h = "0123456789ABCDEF"
    println("${h[i / 16]}${h[i.rem(16)]}")
}

fun main() = A()
