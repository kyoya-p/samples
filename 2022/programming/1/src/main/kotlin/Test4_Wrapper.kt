package test4

import kotlinx.coroutines.*
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream

class Env {
    inner class Pipe {
        val intake = object : PipedOutputStream() {
            override fun write(b: ByteArray) {
                rawStdout.println("write($b)")
                super.write(b)
            }

            override fun write(b: Int) {
                rawStdout.println("write($b)")
                super.write(b)
            }

            override fun write(b: ByteArray, off: Int, len: Int) {
                rawStdout.println("write(${String(b, off, len)})")
                super.write(b, off, len)
            }

        }
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
    val thEnv = launch {
        kotlin.runCatching {
            env.rawStdout.println("Test Environment Start.")
            env.envSvr()
            env.rawStdout.println("Test Environment Complete.")
        }.onFailure { it.printStackTrace(System.err) }
    }

    env.rawStdout.println("Target Start.")
    val r = target()
    env.rawStdout.println("Target Complete.")

    thEnv.join()
    System.setIn(env.rawStdin)
    System.setOut(env.rawStdout)
    return@coroutineScope r
}

fun main() = runBlocking(Dispatchers.Default) {
    stdioEmulatior({
        PrintStream(intake).println("ABC")
        val r = outlet.bufferedReader().readLine()
        rawStdout.println("Thanks to reply [$r]")
    }) {
        main2.main()
    }
}


