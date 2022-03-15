package testEnvironment

import kotlinx.coroutines.*
import test4.stdioEmulatior


fun main() = runBlocking(Dispatchers.Default) {
    stdioEmulatior({
        val N = 6
        val Q = 4
        val rm = ('A' until 'Z').take(N).shuffled().withIndex().associate { it.value to it.index }
        val ans = rm.entries.sortedBy { it.value }.map { it.key }.joinToString("")
        rawStdout.println(ans)
        intake.println("$N $Q")
        val res = let {
            outlet.lineSequence().map { it.split(" ") }.forEach { c ->
                when (c[0]) {
                    "!" -> return@let c[1]
                    "?" -> intake.println(if (rm[c[1][0]]!! > rm[c[2][0]]!!) ">" else "<")
                    else -> throw Exception()
                }
            }
            throw Exception()
        }
        if(res==ans) rawStdout.println("Passed")
    }) {
        main.main()
    }
}


