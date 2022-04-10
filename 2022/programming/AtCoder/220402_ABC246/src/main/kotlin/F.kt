@file:Suppress("unused")

package main.f

// Debugging
val <T> T.p get() = also { System.err.print("$it ") }
val <T> T.pl get() = also { System.err.println(it) }
val <T> Iterator<T>.ple get() = also { forEach { it.pl } }
val <T> Sequence<T>.ple get() = also { forEach { it.pl } }
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

operator fun <T> V2<T>.get(i: Int, j: Int): T = get(j)[i]
operator fun <T> V2<T>.set(i: Int, j: Int, e: T): T = get(j).set(i, e)
typealias V2L = V2<Long>

val String.sp get() = split(" ")
val rln get() = readLine()!!
val rlni get() = rln.toInt()
val rlnl get() = rln.toLong()

val rlnvi get() = rln.sp.mapi
val rlnvl get() = rln.sp.mapl
val rlnvf get() = rln.sp.mapf

val Iterable<String>.mapi get() = map { it.toInt() }
val Iterable<String>.mapl get() = map { it.toLong() }
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
fun rp(n: Int) = (0..n - 1).asSequence()
fun rp(n: Long) = (0..n - 1).asSequence()
fun <R> rp(n: Int, f: (Int) -> R) = rp(n).map(f)
fun <R> rp(n: Long, f: (Long) -> R) = rp(n).map(f)

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

typealias KEYS = List<Char>

val MOD = 998244353L

fun factDiv(n: Long, d: Long = 1L) = (d..n).fold(1L) { a, e -> (a * e).rem(MOD) }
fun nPerm(n: Long, r: Long) = factDiv(n, n - r + 1)
fun nPermRep(n: Long, r: Long) = rp(r).fold(1L) { a, _ -> (a * n).rem(MOD) }
fun nComb(n: Long, r: Long) = if (r > n - r) factDiv(n, r) / factDiv(n - r) else factDiv(n, r) / factDiv(r)
fun nCombRep(n: Long, r: Long) = nComb(n + r - 1, r)

fun <T> List<T>.pick(i: Int) = Triple(get(i), take(i), drop(i + 1))
fun <T> List<T>.perm(n: Int): Sequence<List<T>> = when {
    n == 0 -> sequenceOf(listOf())
    else -> rp(size) { pick(it) }.flatMap { (e, h, t) -> (h + t).perm(n - 1).map { listOf(e) + it } }
}

fun <T> List<T>.permRep(n: Int): Sequence<List<T>> = when {
    n == 0 -> sequenceOf(listOf())
    else -> asSequence().flatMap { e -> perm(n - 1).map { listOf(e) + it } }
}

fun <T> List<T>.comb(n: Int): Sequence<List<T>> = when {
    n == 0 -> sequenceOf(listOf())
    else -> rp(size) { pick(it) }.flatMap { (e, _, t) -> t.comb(n - 1).map { listOf(e) + it } }
}

fun <T> List<T>.combRep(n: Int): Sequence<List<T>> = when {
    n == 0 -> sequenceOf(listOf())
    else -> rp(size) { pick(it) }.flatMap { (e, _, t) -> (t + e).comb(n - 1).map { listOf(e) + it } }
}

//    fun main(): Unit {
fun main(): Unit = testenv.stdioEmulatiors(testEnvs) {
    val (N, L) = rlnvi
    val rows = rp(N).map { rln.toList().distinct() }.toList()
    (1..N).map { n ->
        n.p;p(" ")
        rows.comb(n).map { it.reduce { a, e -> a.intersect(e).toList() } }.map { keys ->
            nPermRep(keys.p.size.toLong(), L.toLong()).rem(MOD).p
        }.sum().rem(MOD).times(n % 2L * 2 - 1).pl
    }.sum().rem(MOD).ppl
}

val te1 = testenv.testEnv {
    intake.println("""
2 2
ab
ac
""".trimIndent())
    outlet.readLine() == "7"
}

val te2 = testenv.testEnv {
    intake.println("""
4 3
abcdefg
hijklmnop
qrstuv
wxyz
""".trimIndent()
    )
    outlet.readLine() == "1352"
}

val te3 = testenv.testEnv {
    intake.println("""
5 1000000000
abc
acde
cefg
abcfh
dghi
""".trimIndent()
    )
    outlet.readLine() == "346462871"
}

//val testEnvs = testEnv_AtCoder("ARC137", "C")
val testEnvs = listOf(te1, te2, te3)
