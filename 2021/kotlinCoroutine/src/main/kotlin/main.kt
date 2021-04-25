import kotlinx.coroutines.*
import java.util.*

fun main(): Unit = runBlocking {
    println("----- main1()")
    main1().join()
    println("----- main2()")
    main2().join()
    println("----- main3()")
    main3().join()

}

suspend fun main1()= coroutineScope {
    tStart = Date().time
    launch {
        job1("T") {
            launch { job1("A") { cancel() } }
            launch { job1("B") {} }
            delay(1000)
        }
    }
}

suspend fun main2()= coroutineScope {
    tStart = Date().time
    launch {
        job1("T") {
            launch { job1("A") {} }
            launch { job1("B") {} }
            delay(300)
            cancel()
        }
    }
}


// GlobalScope.launch{}は終了しない
suspend fun main3()= coroutineScope {
    tStart = Date().time
    launch {
        job1("T") {
            launch { job1("A") { } }
            GlobalScope.launch { job1("B") {} }
            delay(300)
            cancel()
            delay(700)
        }
    }
}

var tStart = Date().time
fun printT(m: String) = println("${Date().time - tStart} $m")
inline suspend fun <R> job1(name: String, op: suspend () -> R) = runCatching {
    printT("<<<<<:$name")
    delay(500)
    val r = op()
    delay(500)
    printT(">>>>>:$name")
    r
}.onFailure { printT(">>>>>:$name Canceled") }.getOrNull()
