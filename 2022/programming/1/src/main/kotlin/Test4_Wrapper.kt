package test4

import kotlinx.coroutines.*
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream

class Env {
    inner class Pipe {
        val intake = PipedOutputStream()
        val outlet = PipedInputStream(intake)
    }

    val rawStdin = System.`in`
    val rawStdout = System.out
    val emuStdin = Pipe()
    val emuStdout = Pipe()

    val intake get() = emuStdin.intake
    val outlet get() = emuStdout.outlet
}

suspend fun <R> stdioEmulatior(envSvr: suspend Env.() -> Unit, target: suspend () -> R) = coroutineScope {
    val env = Env()
    System.setIn(env.emuStdin.outlet)
    System.setOut(PrintStream(env.emuStdout.intake))
    launch { env.envSvr() }
    val r = target()
    System.setIn(env.rawStdin)
    System.setOut(env.rawStdout)
    return@coroutineScope r
}

fun main() = runBlocking(Dispatchers.Default) {
    stdioEmulatior({
        rawStdout.println("T1")
        PrintStream(intake).println("ABC")
        rawStdout.println("T2")
        val r = outlet.bufferedReader().readLine()
        rawStdout.println("T3")
        rawStdout.println("Thanks to reply [$r]")

    }) {
        main.main()
    }
}


