package testEnv

import kotlinx.coroutines.*
import test4.stdioEmulatior
import java.io.PrintStream


fun main() = runBlocking(Dispatchers.Default) {
    stdioEmulatior({
        val N = 3
        val Q = 3
        val rndList = (0 until N).shuffled()

        PrintStream(intake).println("$N $Q")
        rawStdout.println("$N $Q")
        while (true) {
            val c = outlet.bufferedReader().readLine()!!.split(" ")
            rawStdout.println("$c")
            when (c[0]) {
                "!" -> break
                "?" -> when {
                    c[1] > c[2] -> PrintStream(emuStdin.intake).println("<")
                    else -> PrintStream(emuStdin.intake).println(">")
                }
                else -> throw Exception()
            }
        }
    }) {
        main.main()
    }
}


