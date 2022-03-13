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
    val stdoutSync = PipedInputStream(stdout)
    System.setIn(stdin)
    System.setOut(PrintStream(stdout))


    val th = thread {
        stdinIntake.write("xxx x\n".toByteArray())
        val r = stdoutSync.bufferedReader().readLine()
        rawStdout.println("Thanks to reply [$r]")
    }
    rawStdout.println("Test3:")

    main.main()
    rawStdout.println("Test6:")

    System.setIn(rawStdin)
}


