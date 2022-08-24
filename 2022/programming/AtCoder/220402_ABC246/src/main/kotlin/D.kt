@file:Suppress("unused")

package main.d


// Debugging
val <T> T.p get() = also { System.err.print("$it ") }
val <T> T.pl get() = also { System.err.println(it) }
val <T> T.ppl get() = also { println(it) }
fun <T> p(v: T) = System.err.print(v)
fun <T> pl(v: T) = System.err.println(v)
val <T : Collection<T>> T.pv get() = forEach { System.err.println(it) }

// Sortcuts
typealias FBI = (Int) -> Boolean
typealias FBL = (Long) -> Boolean
typealias PLL = Pair<Long, Long>
typealias V<T> = MutableList<T>
typealias VL = V<Long>
typealias V2<T> = MutableList<MutableList<T>>

fun <T, R> V2<T>.rp(f: (j: Int, i: Int, e: T) -> R) =
    forEachIndexed { j, ej -> ej.forEachIndexed { i, ei -> f(j, i, ei) } }

operator fun <T> V2<T>.get(i: Int, j: Int): T = get(j).get(i)
operator fun <T> V2<T>.set(i: Int, j: Int, e: T): T = get(j).set(i, e)
typealias V2L = V2<Long>

val String.sp get() = split(" ")
val rln get() = readLine()!!
val rlni get() = rln.toInt()
val rlnl get() = rln.toLong()
val rlnvi get() = rln.sp.mapi
val rlnvf get() = rln.sp.mapf
val Iterable<String>.mapi get() = map { it.toInt() }
val Iterable<String>.mapf get() = map { it.toFloat() }

// Utils
fun <T : Comparable<T>> max(a: T, b: T) = if (a >= b) a else b
fun <T : Comparable<T>> min(a: T, b: T) = if (a < b) a else b
fun <T : Comparable<T>> max(vararg a: T) = a.max()!!
fun <T : Comparable<T>> min(vararg a: T) = a.min()!!
data class P<T>(var x: T, var y: T)

// Control Structure
fun <T> T.letIf(test: T, t: T) = if (test == this) t else this
fun <T, R> T.letIf(test: T, t: R, f: R) = if (test == this) t else f
fun <T, R> T.letIf(test: (T) -> Boolean, op: (T) -> R) = if (test(this)) op(this) else null
val <T : Any> T.rp get() = generateSequence { this }
fun <T : Any> T.rpWhile(t: (T) -> Boolean) = rp.takeWhile { t(it) }
fun <T, R> T.rp(n: Long, op: (Long, T) -> R) = (0 until n).map { op(it, this) }.toMutableList()
fun <T, R> T.rp(nx: Long, ny: Long, op: (PLL, T) -> R) = rp(nx) { x, e -> e.rp(ny) { y, e2 -> op(x to y, e2) } }

// Snippet
fun bsch(s: Long, e: Long, m: Long = (s + e) / 2, t: FBL): Long =
    if (e <= s) s else if (t(m)) bsch(s, m, t = t) else bsch(m + 1, e, t = t)

fun grp_FW(d: V2L) =
    d.rp { j, i, _ -> d.indices.forEach { k -> if (d[i, k] > d[i, k] + d[k, j]) d[i, j] = d[i, k] + d[k, j] } }

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalStdlibApi::class)
fun <N> grp_01BFS(egs: Map<N, Map<N, Long>>, ns: N, dts: MutableMap<N, Long>) =
    ArrayDeque(listOf(ns)).rpWhile { it.isNotEmpty() }.forEach { q ->
        q.removeFirst().let { n1 ->
            egs[n1]!!.forEach { (n2, w) ->
                (dts[n1]!! + w).letIf({ it < dts[n2]!! }) { d ->
                    dts[n2] = d
                    if (w == 0L) q.addFirst(n2) else q.addLast(n2)
                }
            }
        }
    }


//    fun main(): Unit {
fun main(): Unit = testenv.stdioEmulatiors(testEnvs) {
    val N = rlnl

    val lim = 1_000_000L
    fun f(x: Long, y: Long) = x * x * x + x * x * y + x * y * y + y * y * y
    (0..lim).asSequence().map { x -> f(x, bsch(0, lim) { y -> f(x, y) >= N }) }.min().ppl
}

val te1 = testenv.testEnv {
    intake.println("""
9
""".trimIndent())
    outlet.readLine() == "15"
}

val te2 = testenv.testEnv {
    intake.println("""
0
""".trimIndent()
    )
    outlet.readLine() == "0"
}

val te3 = testenv.testEnv {
    intake.println("""
999999999989449206
""".trimIndent()
    )
    outlet.readLine() == "1000000000000000000"
}

//val testEnvs = testEnv_AtCoder("ARC137", "C")
val testEnvs = listOf(te1, te2, te3)
