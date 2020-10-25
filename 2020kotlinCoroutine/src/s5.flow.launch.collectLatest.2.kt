import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

@Suppress("BlockingMethodInNonBlockingContext")
@ExperimentalCoroutinesApi
suspend fun main() = runBlocking {
    val start = Date().time
    fun now() = "${Date().time - start}"
    channelFlow {
        for (i in 0..4) {
            offer(i)
            delay(200)
        }
    }.collectLatest {
        println("${now()} Start new event $it")
        channelFlow {
            launch { // channelFlowならばProducerScope中でのlaunchも停止できる
                repeat(5) { i ->
                    offer(i)
                    delay(100)
                }
            }
        }.collect { i ->
            println("${now()} j1: $i")
        }
    }
}

