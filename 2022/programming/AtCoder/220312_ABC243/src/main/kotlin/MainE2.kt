package main.e2

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
    }
)


fun main() {
    @Suppress("LocalVariableName")
    fun main() {
        fun graph_FloydWarshall(d: MutableList<MutableList<Long>>) = d.forEachIndexed { i, ei ->
            ei.forEachIndexed { j, c ->
                for (k in 0 until d.size) if (d[i][k] > d[i][k] + d[k][j]) d[i][j] = d[i][k] + d[k][j]
            }
        }
        val (N, M) = readLine()!!.split(" ").map { it.toInt() }
        val INF = Long.MAX_VALUE / 16
        val es = (0..M - 1).map { readLine()!!.split(" ").map { it.toInt() } }
            .map { (a, b, c) -> Triple(a - 1, b - 1, c.toLong()) }
        val d = MutableList(N) { MutableList(N) { INF } }
        es.forEach { (a, b, c) -> d[a][b] = c; d[b][a] = c }
        graph_FloydWarshall(d)
        val res = es.filter { (a, b, c) -> (0 until N).any { k -> d[a][k] + d[k][b] < c } }.count()
        println(res)
    }

// --------------------------------
    stdioEmulatior(envs)
    {
        main()
    }
}
