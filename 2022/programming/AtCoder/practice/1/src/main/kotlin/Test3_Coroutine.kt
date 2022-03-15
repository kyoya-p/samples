package test3

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream

@Suppress("BlockingMethodInNonBlockingContext")
fun main() = runBlocking(Dispatchers.Default) {
    val rawStdin = System.`in`
    val rawStdout = System.out

    val stdinIntake = PipedOutputStream()
    val stdin = PipedInputStream(stdinIntake)
    val stdout = PipedOutputStream()
    val stdoutOutlet = PipedInputStream(stdout)
    System.setIn(stdin)
    System.setOut(PrintStream(stdout))

    launch {
        stdinIntake.write("ABC\n".toByteArray())
        val r = stdoutOutlet.bufferedReader().readLine()
        rawStdout.println("Thanks to reply [$r]")
    }
    main2.main()

    System.setIn(rawStdin)
    System.setOut(rawStdout)
}


