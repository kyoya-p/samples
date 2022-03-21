package main.a

import testenv.stdioEmulatior
import testenv.testEnv
import testenv.testEnv_AtCoder


val environments = listOf(
    testEnv {
        intake.println("""
2 4
            """.trimIndent()
        )
        outlet.readLine() == "1"
    },
    testEnv {
        intake.println(
            """
14 21
            """.trimIndent()
        )
        outlet.readLine() == "5"
    },
    testEnv {
        intake.println(
            """
1 100
            """.trimIndent()
        )
        outlet.readLine() == "99"
    },

    testEnv {
        intake.println(
            """
407833339917496991 742004829320395233
            """.trimIndent()
        )
        outlet.readLine() == "334171489402898242"
    },
    testEnv {
        intake.println(
            """
316535022320294219 886946237258898063
            """.trimIndent()
        )
        outlet.readLine() == "570411214938603843"
    }
)


fun <T> T.syserr() = also { System.err.println(it) }

fun main() {

    // --------------------------------
    fun main() {
        fun math_gcd(m: Long, n: Long): Long? =
            generateSequence(m to n) { (m, n) -> n to m % n }.find { (_, n) -> n == 0L }?.first

        val (L, R) = readLine()!!.split(" ").map { it.toLong() }
        val max = run {
            for (d in R - L downTo 1) for (m in L until R - d + 1) if (math_gcd(m, m + d) == 1L) return@run d
        }
        println(max)
    }
    // --------------------------------

    stdioEmulatior(testEnv_AtCoder("ARC137", "A", debug = true)) {
        main()
    }
}
