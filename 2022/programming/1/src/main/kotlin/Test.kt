package testEnv

import kotlinx.coroutines.*
import test4.stdioEmulatior


fun main() = runBlocking(Dispatchers.Default) {
    stdioEmulatior({
        val N = 3
        val Q = 4
        val rndList = (0 until N).shuffled()
        fun weight(label: String): Int = rndList[label[0] - 'A']
        intake.println("$N $Q")
        repeat(Q) {
            val c = outlet.readLine()!!.split(" ")
            when (c[0]) {
                "!" -> return@repeat
                "?" -> intake.println(if (weight(c[1]) > weight(c[2])) "<" else ">")
                else -> throw Exception()
            }
        }
    }) {
        main.main()
    }
}


