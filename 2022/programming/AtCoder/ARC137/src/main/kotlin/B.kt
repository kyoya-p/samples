package main.b

import testenv.stdioEmulatior
import testenv.testEnv
import testenv.testEnv_AtCoder

fun <T> T.syserr() = also { System.err.println(it) }

fun main() {

    // --------------------------------
    fun main() {
        val N = readLine()!!.toInt()
        val V = readLine()!!.split(" ").map { it.toInt() }
        val c = mutableListOf(0, 0)
        sequence {
            yield(c)
            for (i in 0 until V.size) {
                c[V[i]]++
                yield(c)
            }
        }.forEach { (it[1] - it[0]).syserr() }
        println(c)
    }
// --------------------------------
    val e = testEnv {
        intake.println("5")
        intake.println("0 0 1 1 0 0")
        outlet.readLine() == "3"
    }
    stdioEmulatior(listOfNotNull(e) + testEnv_AtCoder("ARC137", "B")) {
        main()
    }
}
