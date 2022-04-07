@file:Suppress("unused", "EXPERIMENTAL_IS_NOT_ENABLED")

package main.d

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

fun <T, R> T.If(test: (T) -> Boolean, t: R) = if (test(this)) t else null

fun graph_FloydWarshall(d: MutableList<MutableList<Long>>) = d.forEachIndexed { i, ei ->
    ei.forEachIndexed { j, c ->
        for (k in 0 until d.size) if (d[i][k] > d[i][k] + d[k][j]) d[i][j] = d[i][k] + d[k][j]
    }
}


//val testEnvs = testEnv_AtCoder("ARC137", "C")
val testEnvsSample = listOf(testEnv {
    intake.println("INPUT")
    outlet.readLine() == "ANS"
})


val testSample1 = testEnv {
    intake.println("5")
    intake.println("1 3")
    intake.println("3 5")
    intake.println("....#")
    intake.println("...#.")
    intake.println(".....")
    intake.println(".#...")
    intake.println("#....")
    outlet.readLine() == "3"
}

val testSample2 = testEnv {
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

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalStdlibApi::class)
fun main(): Unit = stdioEmulatiors(listOf(testSample2)) {
    //    fun main(): Unit {
    data class Node(val x: Int, val y: Int, val slash: Int /* 1='\', -1='/', 0=start */)

    val N = rlni
    val nStart = rlnvi.run { Node(this[0], this[1], 0) }
    val (ex, ey) = rlnvi

    val nodes = (1..N).flatMap { y ->
        rln.toList().zip((1..N)).flatMap { (e, x) ->
            when {
                x == nStart.x && y == nStart.y -> listOf(Node(x, y, 0))
//                x == ex && y == ey -> listOf(Node(x, y, E))
                e == '#' -> listOf()
                else -> listOf(Node(x, y, 1), Node(x, y, -1))
            }
        }
    }.distinct()

    data class Edge<N>(val n1: N, val n2: N, val w: Long)

    val edges = nodes.associateWith { n ->
        listOf(P(-1, -1), P(-1, 1), P(1, -1), P(1, 1))
            .map { p -> Edge(n, Node(n.x + p.x, n.y + p.y, p.x * p.y), if (n.slash != p.x * p.y) 1L else 0) }
            .filter { nodes.contains(it.n2) }
    }
    val dist = nodes.associateWith { 1_000_000_000_000L }.toMutableMap()

    fun <N> graph_01BFS(edges: Map<N, List<Edge<N>>>, start: N, distList: MutableMap<N, Long>) {
        distList[start] = 0L
        val q = ArrayDeque(listOf(start))
        while (q.isNotEmpty()) {
            "${q.size}\r".p
            val n = q.removeFirst()
            edges[n]!!.forEach { eg ->
                val d = distList[n]!! + eg.w
                if (d < distList[eg.n2]!!) {
                    distList[eg.n2] = d
                    when {
                        eg.w == 0L -> q.addFirst(eg.n2)
                        else -> q.addLast(eg.n2)
                    }
                }
            }
        }
    }

    val m = 0.r(N, N) { _, e -> mutableListOf(Long.MAX_VALUE, Long.MAX_VALUE) }
    graph_01BFS(edges, nStart, dist)

    dist.forEach { k, v -> if (v != 1_000_000_000_000L) m[k.y - 1][k.x - 1][if (k.slash == 1) 0 else 1] = v }
    m.onEach { it.onEach { it.min().let { if (it != Long.MAX_VALUE) "$it" else "." }.p }; pl("") }

    min(dist[Node(ex, ey, 1)]!!, dist[Node(ex, ey, -1)]!!).ppl
}


fun <T, R> T.r(n: Int, op: (Int, T) -> R) = (0 until n).map { op(it, this) }.toMutableList()
fun <T, R> T.r(nx: Int, ny: Int, op: (Pair<Int, Int>, T) -> R) = r(nx) { x, e -> e.r(ny) { y, e2 -> op(x to y, e2) } }
