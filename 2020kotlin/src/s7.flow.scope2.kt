import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.*

@ExperimentalCoroutinesApi
fun main(): Unit = runBlocking {
    println("sample0-----")
    reset()
    srcFlow(3, 100).collect { aTask(it) } // aTaskが完了するまで次のFlowは保留

    println("sample1-----")
    reset()
    srcFlow(3, 100).collectLatest { aTask(it) } //collectLatestによって実行中のaTask()はcancelされる

    println("sample2-----")
    reset()
    val j2 = launch {
        srcFlow(3, 100).collectLatest { aTask(it) }
    }
    delay(200)
    j2.cancel() //全体フローも200msで明示的にcancelされる

    println("sample3-----")
    reset()
    val j3 = launch {
        srcFlow(3, 100).collect { launch { aTask(it) } } // aTask()は同時実行される
    }
    delay(500)
    j3.cancel()

    println("sample4-----")
    reset()
    val j4 = launch {
        launch { srcFlow(3, 100).collectLatest { aTask(it) } }
        launch { srcFlow(3, 100).collectLatest { aTask(it + 10) } }
    }
    delay(200)
    j4.cancel() //複数のフローも200msで明示的にcancelされる

}

var start = 0L
fun reset() {
    start = Date().time
}

fun rap() = "000${(Date().time - start) % 10000}".takeLast(4)

suspend fun aTask(id: Int) {
    println("${rap()} Start task $id")
    repeat(5) { j ->
        println("${rap()} task=$id : $j ${if (j == 4) 'x' else ' '}")
        delay(100)
    }
}

@ExperimentalCoroutinesApi
fun srcFlow(times: Int, inteval: Long) = callbackFlow {
    repeat(times) {
        offer(it)
        delay(inteval)
    }
    close()
    awaitClose()
}

@ExperimentalCoroutinesApi
fun Flow<Int>.rescheduleFlow(x: Int) = channelFlow {
    collectLatest { sc ->
        launch {
            for (i in 0..x) {
                offer(sc + i)
                delay(100)
            }
        }
    }
    close()
    awaitClose()
}

