import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.*

@ExperimentalCoroutinesApi
fun main(): Unit = runBlocking {
    // aTaskが完了するまで次のFlowは保留
    println("sample0-----")
    reset()
    srcFlow(3, 100).collect { aTask(it) }

    //collectLatestによって実行中のaTask()はcancelされる
    println("sample1-----")
    reset()
    srcFlow(3, 100).collectLatest { aTask(it) }

    // CoroutinScopeの外部からのキャンセル
    println("sample2-----")
    reset()
    val j2 = launch {
        srcFlow(3, 100).collectLatest { aTask(it) }
    }
    delay(200)
    j2.cancel() //全体フローも200msで明示的にcancelされる


    // collectブロックをブロックしない
    println("sample3-----")
    reset()
    val j3 = launch {
        srcFlow(10, 100).collect { launch { aTask(it * 100) } } // aTask()は同時実行される
    }
    delay(500)
    j3.cancel()
    delay(100)

    // 複数の子Coroutineの停止
    println("sample4-----")
    reset()
    val j4 = launch {
        launch { srcFlow(3, 100).collectLatest { aTask(it) } }
        launch { srcFlow(3, 100).collectLatest { aTask(it + 10) } }
    }
    delay(200)
    j4.cancel() //複数フローも200msで明示的にcancelされる

}

var start = 0L
fun reset() {
    start = Date().time
}

fun rap() = "000${(Date().time - start) % 10000}".takeLast(4)

suspend fun aTask(id: Int) {
    println("${rap()} Start task $id")
    try {
        repeat(5) { j ->
            println("${rap()} task=$id : $j ${if (j == 4) 'x' else ' '}")
            delay(100)
        }
    } finally {
        println("${rap()} terminated task(${id})")
    }
}

@ExperimentalCoroutinesApi
suspend fun srcFlow(times: Int, inteval: Long) = callbackFlow {
    repeat(times) {
        offer(it)
        delay(inteval)
    }
    close()
    awaitClose()
}
