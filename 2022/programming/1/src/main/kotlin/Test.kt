package test

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream


fun main() {
    val stdin = System.`in`
    val stdout = System.out

    val siba = ByteArrayInputStream("3 3".toByteArray())
    val soba = ByteArrayOutputStream()

    System.out.println("Test:")

    System.setIn(siba)
    System.setOut(PrintStream(soba))
    main.main()
    System.setIn(stdin)
    System.setOut(stdout)

    println("[${soba}]")
}


