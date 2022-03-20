package main.e

import testenv.stdioEmulatior
import testenv.testEnv

val ex = testEnv {
    debug = true
    intake.println(
        """
5 6
1 2 4
1 4 1
1 5 9
2 5 1
2 3 1
3 4 1
            """.trimIndent()
    )
    outlet.readLine() == "2"
}

val envs = listOf(
    testEnv {
        intake.println("""
3 3
1 2 2
2 3 3
1 3 6
            """.trimIndent()
        )
        outlet.readLine() == "1"
    },
    testEnv {
        intake.println(
            """
5 4
1 3 3
2 3 9
3 5 3
4 5 3
            """.trimIndent()
        )
        outlet.readLine() == "0"
    },
    testEnv {
        debug = true
        intake.println(
            """
5 10
1 2 71
1 3 9
1 4 82
1 5 64
2 3 22
2 4 99
2 5 1
3 4 24
3 5 18
4 5 10
            """.trimIndent()
        )
        outlet.readLine() == "5"
    },
    ex
)


fun main() {
    open class Edge<NODE>(val a: NODE, val b: NODE)
    data class EdgeND<NODE>(val a1: NODE, val b1: NODE) : Edge<NODE>(a1, b1) { // 無方向グラフ
        override fun hashCode(): Int = a.hashCode() + b.hashCode()
        override fun equals(other: Any?): Boolean =
            (other is Edge<*>) && (a == other.a && b == other.b || a == other.b && b == other.a)
    }

    data class EdgeWeight<EDGE>(val e: EDGE, val w: Int)

    fun <NODE> graphLibFloydWarshall(edgeList: List<EdgeWeight<EdgeND<NODE>>>): Map<Edge<NODE>, Int> {
        val edgeMap: MutableMap<Edge<NODE>, Int> = edgeList.associate { it.e to it.w }.toMutableMap()
        val nodeList = edgeList.asSequence().flatMap { sequenceOf(it.e.a, it.e.b) }.distinct()
        nodeList.flatMap { i ->
            nodeList.flatMap { j ->
                nodeList.map { k -> listOf(EdgeND(i, j), EdgeND(i, k), EdgeND(k, j)) }
            }
        }.distinct().forEach { ij_ik_kj ->
            val (eij, _, _) = ij_ik_kj
            val (ij, ik, kj) = ij_ik_kj.map { edgeMap[it] }
            //System.err.print("$eij=$ij $eik=$ik $ekj=$kj")
            when {
                ik != null && kj != null && ij != null -> if (ij > ik + kj) edgeMap[eij] = ik + kj
                ik != null && kj != null && ij == null -> edgeMap[eij] = ik + kj
            }
            //System.err.print(" / ${ij_ik_kj[0]}=$eik $eik=$ik $ekj=$kj")
            //System.err.println()
        }
        return edgeMap
    }

    //fun edgeWeights(n: Int) = MutableList(n) { MutableList(n) { 1000000000 } }
    fun MutableList<MutableList<Int>>.floydWarshall() {
        val e = this
        asSequence().withIndex().flatMap { (i, n1) ->
            n1.asSequence().withIndex().flatMap { (j, wij) ->
                (0 until e.size).asSequence().map { k ->
                    val (wik, wkj) = listOf(e[i][k], e[k][j])
                    if (wij > wik + wkj) e[i][j] = wik + wkj
                }
            }
        }
    }


    class MutableMatrix2<T : Number>(val n: Int, val init: (Int, Int) -> T) {
        val m = MutableList(n * n) { init(it % n, it / n) }
        operator fun get(n1: Int, n2: Int) = m[n1 + n2 * n]
        operator fun get(node: Pair<Int, Int>) = m[node.first + node.second * n]
        operator fun set(n1: Int, n2: Int, w: T) = { m[n1 * n + n2] = w }()
        fun clone() = MutableMatrix2(n) { i, j -> m[i + j * n] }
        fun replace(op: (Pair<Int, Int>, T) -> T) = apply { m.mapIndexed { i, e -> m[i] = op(i % n to i / n, e) } }
        fun <R> mapIndexed(op: (Pair<Int, Int>, T) -> R) = m.mapIndexed { i, e -> op(i % n to i / n, e) }
        fun <R> forEachIndexed(op: (Pair<Int, Int>, T) -> R) = m.forEachIndexed { i, e -> op(i % n to i / n, e) }

        inner class Entry(val i: Int, val j: Int, val w: T)

        val entries get() = m.mapIndexed { i, e -> Entry(i % n, i / n, e) }
    }

    @Suppress("LocalVariableName")
    fun main() {
        val (N, M) = readLine()!!.split(" ").map { it.toInt() }
        val W = MutableMatrix2(N) { i, j -> 1000000000 }
        repeat(M) {
            val (a, b, w) = readLine()!!.split(" ").map { it.toInt() }
            W[a - 1, b - 1] = w
        }

        val Wmin = W.clone().apply {
            forEachIndexed { (i, j), wij ->
                (0 until N).map { k ->
                    val (wik, wkj) = listOf(W[i, k], W[k, j])
                    if (wij > wik + wkj) this[i, j] = wik + wkj else wij
                }
            }
        }
        val r =
            Wmin.entries.filter { it.w < 1000000000 }
                .map { "(${it.i+1},${it.j+1})=${it.w}" }
                .joinToString(prefix = "[", postfix = "]")
        System.err.println(r)
    }

    fun main2() {
        val (_, M) = readLine()!!.split(" ").map { it.toInt() }
        val D =
            (1..M).map { readLine()!!.split(" ").map { it.toInt() } }
                .map { p -> EdgeWeight(EdgeND(p[0], p[1]), p[2]) }
        val Dmap = D.associate { it.e to it.w }
        //System.err.println(Dmap)

        val Dmin = graphLibFloydWarshall(D)

        val Dr = Dmin.filter { (e, w) -> Dmap[e] == w }
        //System.err.println(Dmin)
        //System.err.println(Dr)
        //System.err.println(D.size - Dr.size)
        println(D.size - Dr.size)
    }

    stdioEmulatior(envs)
    {
        main()
    }
}
