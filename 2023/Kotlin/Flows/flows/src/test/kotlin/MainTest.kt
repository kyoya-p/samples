import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds


class MainTest {
    @Test
    fun test1_Channel_1() = runTest {
        val channel = Channel<Int>()
        launch {
            repeat(5) {
                println("sending($it)")
                channel.send(it)
            }
            channel.close()
        }
        repeat(5) { println(channel.receive()) }
    }

    @Test
    fun test1_Channel_2() = runTest {
        val channel = Channel<Int>()
        launch {
            repeat(5) {
                println("sending($it)")
                channel.send(it)
            }
            channel.close()
        }
        // receive()されない場合send()はブロック
        // repeat(5) { println(channel.receive()) }
    }

    @Test
    fun test2_Flow_1() = runTest {
        val multiResultFunction = flow {
            repeat(5) {
                println("sending($it)")
                emit(it)
            }
        }
        multiResultFunction.collect { println(it) }
    }

    @Test
    fun test2_Flow_2() = runTest {
        flow {
            repeat(5) {
                println("sending($it)")
                emit(it)
            }
        }
        // collect()されない場合send()を送信するロジックも実行されない
//        multiResultFunction.collect { println(it) }
    }

    @Test
    fun test3_SharedFlow() = runTest {
        val coldFlow = flow {
            repeat(5) {
                println("sending($it)")
                emit(it)
                delay(1.seconds)
            }
        }
        val sharedFlow = coldFlow.shareIn(this, started = SharingStarted.WhileSubscribed(), replay = 0)
        launch { sharedFlow.collect { println("A=$it") } }
        delay(2.seconds)
        launch { sharedFlow.collect { println("B=$it") } }
    }
}