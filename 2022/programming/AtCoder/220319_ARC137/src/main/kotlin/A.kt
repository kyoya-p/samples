package main.a

import testenv.stdioEmulatiors
import testenv.testEnv_AtCoder

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

    stdioEmulatiors(testEnv_AtCoder("ARC137", "A", debug = true)) {
        main()
    }
}
