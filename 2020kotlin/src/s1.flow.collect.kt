import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import java.util.*

@ExperimentalCoroutinesApi
suspend fun main() {
    channelFlow {
        for (i in 0..999 step 100) {
            offer(i)
            delay(5_000)
        }
    }.collect {
        for (i in 0..9) {
            println("${Date()} $it.$i")
            delay(1000)
        }
        println("cmpl.")
    }
}
