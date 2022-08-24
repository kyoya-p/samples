import kotlinx.coroutines.*
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import java.util.*

@Suppress("BlockingMethodInNonBlockingContext")
@ExperimentalCoroutinesApi
suspend fun main() = runBlocking {
    // runBlockingによりContextをlaunchが実行できる
    channelFlow {
        for (i in 0..999 step 100) {
            offer(i)
            delay(5_000)
        }
    }.collectLatest { // 以前のFlowを停止し新しいFlowを実行する
        launch {// ただし、このブロックは別のContextで実行されるのでcollectLatest()によって停止させられることはない
            for (i in 0..9) {
                println("${Date()} $it.$i")
                if (i == 3) cancel()
                delay(1000)
            }
            println("cmpl launch() $it") // ただし、i==3の時キャンセルされるのでここまでは来ない
        }
        println("escape collect() $it")
    }
}
