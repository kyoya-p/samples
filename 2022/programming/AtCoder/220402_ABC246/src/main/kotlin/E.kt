@file:Suppress("unused", "EXPERIMENTAL_IS_NOT_ENABLED")

package main.e

import testenv.stdioEmulatiors
import testenv.testEnv

// Debugging
val <T> T.p get() = also { System.err.print(it) }
val <T> T.pl get() = also { System.err.println(it) }
val <T> T.ppl get() = also { println(it) }
fun <T> p(v: T) = System.err.print(v)
fun <T> pl(v: T) = System.err.println(v)
val <T : Collection<T>> T.pv get() = forEach { System.err.println(it) }

// Sortcuts
val rln get() = readLine()!!
val rlni get() = rln.toInt()
val String.sp get() = split(" ")
val Iterable<String>.mapi get() = map { it.toInt() }
val Iterable<String>.mapf get() = map { it.toFloat() }
val rlnvi get() = rln.sp.mapi
val rlnvf get() = rln.sp.mapf

// Utils
fun <T : Comparable<T>> max(a: T, b: T) = if (a >= b) a else b
fun <T : Comparable<T>> min(a: T, b: T) = if (a < b) a else b
fun <T : Comparable<T>> max(vararg a: T) = a.max()!!
fun <T : Comparable<T>> min(vararg a: T) = a.min()!!
data class P<T>(var x: T, var y: T)

fun <T> T.letIf(test: T, t: T) = if (test == this) t else this
fun <T, R> T.letIf(test: T, t: R, f: R) = if (test == this) t else f
fun <T, R> T.letIf(test: (T) -> Boolean, op: (T) -> R) = if (test(this)) op(this) else null

fun graph_FloydWarshall(d: MutableList<MutableList<Long>>) = d.forEachIndexed { i, ei ->
    ei.forEachIndexed { j, c ->
        for (k in 0 until d.size) if (d[i][k] > d[i][k] + d[k][j]) d[i][j] = d[i][k] + d[k][j]
    }
}


@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalStdlibApi::class)
fun main(): Unit = stdioEmulatiors(listOf(testSample1, testSample2, testSample3)) {
    //    fun main(): Unit {
    data class Node(val x: Int, val y: Int, val slash: Int /* 1='\', -1='/', 0=start */)

    val N = rlni
    val nStart = rlnvi.run { Node(this[0], this[1], 0) }
    val (ex, ey) = rlnvi
    val MAX = 1_000_000_000_000L
    val nodes = (1..N).flatMap { y ->
        rln.toList().zip((1..N)).flatMap { (e, x) ->
            when {
                x == nStart.x && y == nStart.y -> listOf(Node(x, y, 0))
                e == '#' -> listOf()
                else -> listOf(Node(x, y, 1), Node(x, y, -1))
            }
        }
    }.distinct()

    data class Edge<N>(val n1: N, val n2: N, val w: Long)
    val dist = nodes.associateWith { MAX }.toMutableMap()

    val edges2 = nodes.associateWith { n ->
        listOf(P(-1, -1), P(-1, 1), P(1, -1), P(1, 1))
            .associate { p -> Node(n.x + p.x, n.y + p.y, p.x * p.y) to if (n.slash != p.x * p.y) 1L else 0 }
            .filter { nodes.contains(it.key) }
    }
    dist[nStart] = 0L
    graph_01BFS_1(edges2, nStart, dist)

    val m = 0.rep(N, N) { _, e -> mutableListOf(Long.MAX_VALUE, Long.MAX_VALUE) }
    dist.forEach { k, v -> if (v != MAX) m[k.y - 1][k.x - 1][if (k.slash == 1) 0 else 1] = v }
    m.onEach { it.onEach { it.min().let { it.letIf(Long.MAX_VALUE, ".", "$it") }.p }; pl("") }
    min(dist[Node(ex, ey, 1)]!!, dist[Node(ex, ey, -1)]!!).letIf(MAX, -1).ppl
}

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalStdlibApi::class)
fun <N> graph_01BFS_1(edges: Map<N, Map<N, Long>>, ns: N, dists: MutableMap<N, Long>) =
    ArrayDeque(listOf(ns)).loop.takeWhile { it.isNotEmpty() }.forEach { q ->
        q.removeFirst().let { n1 ->
            edges[n1]!!.forEach { (n2, w) ->
                (dists[n1]!! + w).letIf({ it < dists[n2]!! }) { d ->
                    dists[n2] = d
                    if (w == 0L) q.addFirst(n2) else q.addLast(n2)
                }
            }
        }
    }


val <T : Any> T.loop get() = generateSequence { this }
fun <T, R> T.rep(n: Int, op: (Int, T) -> R) = (0 until n).map { op(it, this) }.toMutableList()
fun <T, R> T.rep(nx: Int, ny: Int, op: (Pair<Int, Int>, T) -> R) =
    rep(nx) { x, e -> e.rep(ny) { y, e2 -> op(x to y, e2) } }





//val testEnvs = testEnv_AtCoder("ARC137", "C")
val testEnvsSample = listOf(testEnv {
    intake.println("INPUT")
    outlet.readLine() == "ANS"
})


val testSample1 = testEnv {
    intake.println("""
5
1 3
3 5
....#
...#.
.....
.#...
#....
    """.trimIndent())
    outlet.readLine() == "3"
}

val testSample2 = testEnv {
    intake.println("""
4
3 2
4 2
....
....
....
....
    """.trimIndent())
    outlet.readLine() == "-1"
}

val testSample3 = testEnv {
    intake.print("""
18
18 1
1 18
..................
.####.............
.#..#..####.......
.####..#..#..####.
.#..#..###...#....
.#..#..#..#..#....
.......####..#....
.............####.
..................
..................
.####.............
....#..#..#.......
.####..#..#..####.
.#.....####..#....
.####.....#..####.
..........#..#..#.
.............####.
..................

""".trimIndent()
    )
    outlet.readLine() == "9"
}

