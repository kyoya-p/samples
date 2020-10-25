import kotlinx.coroutines.*
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
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
    }.collect {
        launch { // このブロックを別のContextで実行。launch自信はすぐ終了し次のFlowを待つ。
            for (i in 0..9) {
                println("${Date()} $it.$i")
                delay(1000)
            }
            println("cmpl. $it") //次のFLowがきても中断されることはない
        }
    }
}
