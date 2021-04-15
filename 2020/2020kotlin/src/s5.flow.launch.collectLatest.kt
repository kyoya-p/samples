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
        repeat(5) { i ->
            println("${now()} j1: $it $i")
            delay(100)
        }
    }
}

