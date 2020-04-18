package BSSim

inline fun <T> List<T>.pickTop(n: Int): Sequence<Pair<List<T>, List<T>>> = if (n <= size) sequenceOf(take(n) to drop(n)) else sequenceOf() //1枚ずつ取り出し、最初に取り出したものが[0]
inline fun <T> List<T>.pickBottom(n: Int): Sequence<Pair<List<T>, List<T>>> = if (n <= size) sequenceOf(drop(n).reversed() to take(n)) else sequenceOf() //1枚ずつ取り出し、最初に取り出したものが[0]
inline fun <T> List<T>.putTop(i: List<T>): List<T> = i.reversed() + this //[0]を最初として順に1枚ずつ置く。
inline fun <T> List<T>.putBottom(i: List<T>): List<T> = this + i //[0]を最初として順に1枚ずつ置く。

inline fun Int.if0(op: () -> Int) = if (this == 0) op() else this
inline fun min(a: Int, b: Int) = if (a >= b) b else a
inline fun max(a: Int, b: Int) = if (a >= b) a else b

// for Debug
fun <T> T.pln(mark: String = "") = also { println("${mark}${this} ") }
fun <T> T.print() = also { print("$this ") }
fun <T, R> Sequence<T>.pln(op: T.() -> R) = toList().also { it.mapIndexed { i, it -> it.op().pln("$i:") } }.asSequence()
fun <T> T.printh() = also { shortHash().print() }
infix fun <T> T.assert(test: T) = also {
    if (this != test) {
        println("F:${this}.${this.shortHash()}==${test}.${test.shortHash()}")
        throw Exception()
    }
}

inline fun <T> T.assert(op: T.() -> Boolean) = also {
    if (!op()) {
        println("F:${this}.${this.shortHash()} ")
        throw Exception()
    }
}

fun <T> T.shortHash() = ("00" + hashCode().toUInt().toString(16)).let { it.substring(it.length - 3) }

fun <T> List<T>.only1choice() = if (size == 1) this[0] else throw Exception("This must take one choice.")


infix fun <T> T.assertNot(test: T) = also {
    if (this == test) {
        println("F:${this}.${this.shortHash()}==${test}.${test.shortHash()}")
        throw Exception()
    }
}

