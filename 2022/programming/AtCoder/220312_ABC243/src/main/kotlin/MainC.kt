package main.c

import testenv.stdioEmulatior
import testenv.testEnv


val env = testEnv {
    intake.println(
        """3
2 3
1 1
4 1
RRL
    """.trimIndent()
    )
    outlet.readLine() == "Yes"
}

fun main() {
    fun main() {
        data class P(val x: Int, val y: Int, val vx: Int)

        val N = readLine()!!.toInt()
        val S0 = (0 until N).map {
            readLine()!!.split(" ").map { it.toInt() }
        }.map { P(it[0], it[1], 0) }.toList()
        val LR = readLine()!!
        val S = S0.zip(LR.toList()).map { (p, lr) -> P(p.x, p.y, if (lr == 'L') -1 else 1) }

        val SY = S.groupBy { it.y }.values
        val res = SY.any { Sy ->
            if (Sy.size <= 1) return@any false
            val SRmin = Sy.filter { it.vx > 0 }.minBy { it.x } ?: return@any false
            val SLmax = Sy.filter { it.vx < 0 }.maxBy { it.x } ?: return@any false
            System.err.println(SRmin)
            System.err.println(SLmax)
            SRmin.x < SLmax.x
        }
        println(if (res) "Yes" else "No")
    }

    stdioEmulatior(env) {
        main()
    }
}
