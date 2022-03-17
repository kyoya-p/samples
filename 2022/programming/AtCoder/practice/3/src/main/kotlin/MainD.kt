package main.d

import testenv.stdioEmulatior
import testenv.testEnv


val envs = listOf(
    testEnv {
        intake.println("""
            3 2
            URL
            """.trimIndent())
        println(outlet.readLine() == "6")
    },
    testEnv {
        intake.println("""
            4 500000000000000000
            RRUU
            """.trimIndent())
        println(outlet.readLine() == "500000000000000000")
    },
    testEnv {
        intake.println("""
            30 123456789
            LRULURLURLULULRURRLRULRRRUURRU
            """.trimIndent())
        println(outlet.readLine() == "126419752371")
    }
)

fun main() {
    fun main() {
        val (N, X) = readLine()!!.split(" ")
        System.err.println(N)
        System.err.println(X)
        val D = readLine()!!
        fun String.toBinList() =
            generateSequence(toLong()) { it / 2 }.takeWhile { it >= 1 }.map { (it % 2) == 1L }.toList().reversed()
        System.err.println(X.toBinList())
        val res = D.toList().fold(X.toBinList()) { r, e ->
            when (e) {
                'L' -> r + false
                'R' -> r + true
                else -> r.dropLast(1)
            }
        }

        fun List<Int>.toLong() = fold(0L) { a, e -> a * 2 + e }
        fun List<Boolean>.toLong() = fold(0L) { a, e -> a * 2 + if (e) 1 else 0 }
        println(res.toLong())
        Long.MAX_VALUE
    }
    stdioEmulatior(envs) {
        main()
    }
}
