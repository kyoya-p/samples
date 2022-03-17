package testEnvironment

import testenv.stdioEmulatior
import testenv.testEnv

val env = testEnv {
    val N = 6
    val Q = 4
    val rm = ('A' until 'Z').take(N).shuffled().withIndex().associate { it.value to it.index }
    val ans = rm.entries.sortedBy { it.value }.map { it.key }.joinToString("")
    println(ans)
    intake.println("$N $Q")
    val res = run {
        outlet.lineSequence().map { it.split(" ") }.forEach { c ->
            when (c[0]) {
                "!" -> return@run c[1]
                "?" -> intake.println(if (rm[c[1][0]]!! > rm[c[2][0]]!!) ">" else "<")
                else -> throw Exception()
            }
        }
        throw Exception()
    }
    if (res == ans) println("Passed") else println("Failed")
}

fun main() {
    stdioEmulatior(env) {
        main.main()
    }
}


