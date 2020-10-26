import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import java.util.*

@ExperimentalCoroutinesApi
suspend fun main() {
    channelFlow {
        for (i in 0..999 step 100) {
            offer(i)
            delay(5_000)
        }
    }.collectLatest {
        for (i in 0..9) {
            println("${Date()} $it.$i")
            delay(1000)
        }
        println("cmpl.") //5秒で次のFlowにより中断されるのでここまで来ない(最後を除き)
    }
}
