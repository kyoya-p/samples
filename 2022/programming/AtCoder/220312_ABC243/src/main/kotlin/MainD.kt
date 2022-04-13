package main.d

import testenv.stdioEmulatior
import testenv.testEnv


val envs = listOf(
    testEnv {
        intake.println(
            """
            3 2
            URL
            """.trimIndent()
        )
        outlet.readLine() == "6"
    },
    testEnv {
        intake.println(
            """
            4 500000000000000000
            RRUU
            """.trimIndent()
        )
        outlet.readLine() == "500000000000000000"
    },
    testEnv {
        debug = true
        intake.println(
            """
            30 123456789
            LRULURLURLULULRURRLRULRRRUURRU
            """.trimIndent()
        )
        outlet.readLine() == "126419752371"
    }
)

fun main() {
    fun main() {
        val (_, X) = readLine()!!.split(" ")
        val D = readLine()!!

        fun String.toBinList() =
            generateSequence(toLong()) { it / 2 }.takeWhile { it >= 1 }.map { if ((it % 2) == 1L) '1' else '0' }
                .toList().reversed()
        //System.err.println(X.toBinList().joinToString(""))
        val sb = StringBuffer(X.toBinList().joinToString(""))
        D.forEach { e ->
            when (e) {
                'L' -> sb.append('0')
                'R' -> sb.append('1')
                else -> sb.delete(sb.length-1,sb.length)
            }
            //System.err.println(sb)
        }

        fun List<Char>.toLong() = fold(0L) { a, e -> a * 2 + if (e == '1') 1 else 0 }
        println(sb.toList().toLong())
        Long.MAX_VALUE
    }
    stdioEmulatior(envs) {
        main()
    }
}
