package main.e

import testenv.stdioEmulatior
import testenv.testEnv


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
    }
)

fun main() {

    data class Edge<NODE>(val a: NODE, val b: NODE)
    data class EdgeWeight<NODE>(val e: Edge<NODE>, val w: Int)


    fun <NODE> graphLibFloydWarshall(edgeList: List<EdgeWeight<NODE>>): Map<Edge<NODE>, Int> {
        val edgeMap: MutableMap<Edge<NODE>, Int> = edgeList.associate { it.e to it.w }.toMutableMap()
        val nodeList = edgeList.flatMap { listOf(it.e.a, it.e.b) }.distinct()
        for (i in nodeList) {
            for (j in nodeList) {
                for (k in nodeList) {
                    val (ij, ik, kj) = listOf(Edge(i, j), Edge(i, k), Edge(k, j)).map { edgeMap[it] }
//                    when{
//                        ik != null && kj != null && ij != null && ij > ik + kj -> edgeMap[Edge(i, j)] = ik + kj
//                        ik != null && kj != null -> edgeMap[Edge(i, j)] = ik + kj
//                    }

                    if (ik != null && kj != null) {
                        if (ij != null) {
                            if (ij > ik + kj) edgeMap[Edge(i, j)] = ik + kj
                        } else {
                            edgeMap[Edge(i, j)] = ik + kj
                        }
                    }
                }
            }
        }
        return edgeMap
    }

    @Suppress("LocalVariableName")
    fun main() {
        val (_, M) = readLine()!!.split(" ").map { it.toInt() }
        val D =
            (1..M).map { readLine()!!.split(" ").map { it.toInt() } }.map { p -> EdgeWeight(Edge(p[0], p[1]), p[2]) }
        val Dmap = D.associate { it.e to it.w }

        val Dmin = graphLibFloydWarshall(D)

        val Dr = Dmin.filter { (e, w) -> Dmap[e] == w }
        System.err.println(Dmap)
        System.err.println(Dmin)
        System.err.println(Dr)
        System.err.println(D.size - Dr.size)
        println(D.size - Dr.size)
    }
    stdioEmulatior(envs) {
        main()
    }
}
