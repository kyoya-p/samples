package test2

import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream
import kotlin.concurrent.thread


fun main() {
    val rawStdin = System.`in`
    val rawStdout = System.out

    val stdinIntake = PipedOutputStream()
    val stdin = PipedInputStream(stdinIntake)
    val stdout = PipedOutputStream()
    val stdoutDrain = PipedInputStream(stdout)
    System.setIn(stdin)
    System.setOut(PrintStream(stdout))

    thread {
        stdinIntake.write("ABC\n".toByteArray())
        val r = stdoutDrain.bufferedReader().readLine()
        rawStdout.println("Thanks to reply [$r]")
    }
    main2.main()

    System.setIn(rawStdin)
    System.setOut(rawStdout)
}


