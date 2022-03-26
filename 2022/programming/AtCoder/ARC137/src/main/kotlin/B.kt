package main.b

import testenv.stdioEmulatior
import testenv.testEnv
import testenv.testEnv_AtCoder
import java.lang.Integer.max
import java.lang.Integer.min

fun <T> T.err() = also { System.err.print("[$it]") }
fun <T> T.errln() = also { System.err.println(it) }

fun main() {

    // --------------------------------
    fun main() {
        val N = readLine()!!.toInt()
        val V = readLine()!!.split(" ").map { it.toInt() }
        val c = mutableListOf(0, 0)
        var dmin = 0
        var dmax = 0
        for (e in V) {
            c[e]++
            val d = c[1] - c[0]
            dmin = min(dmin, d)
            dmax = max(dmax, d)
            "$e : $dmin ~ $dmax : ${dmax - dmin}".errln()
        }
        println(dmax - dmin + 2)
    }
// --------------------------------
    val e = testEnv {
        intake.println("5")
        intake.println("0 0 0")
        outlet.readLine() == "4"
    }
    stdioEmulatior(//listOfNotNull(e) +
        testEnv_AtCoder("ARC137", "B")
    ) {
        main()
    }
}
