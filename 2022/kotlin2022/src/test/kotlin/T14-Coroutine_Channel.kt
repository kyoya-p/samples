import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime


@ExperimentalTime
@Suppress("NonAsciiCharacters")
class `T14-Coroutine_Channel` {

    @Test
    fun `t01-チャネル`(): Unit = runBlocking {
        val ch = Channel<Int>()
        launch {
            while (true) {
                println(ch.receiveCatching().onFailure { return@launch }.getOrNull())
            }
        }
        launch {
            for (i in 2 downTo 0) {
                ch.send(i)
            }
            ch.close()
        }
    }

    @Test
    fun `t02-スロットル付きQueue`(): Unit = runBlocking {
        val queIn = Channel<Int>(5)
        val queOut = Channel<Int>()
        launch {
            runCatching {
                while (true) {
                    queOut.send(queIn.receive())
                    delayUntilNextPeriod(50.milliseconds)
                }
            }.getOrNull()
            queOut.close()
        }
        stopWatch { w ->
            launch {
                var last = w.now()
                while (true) {
                    val q = queOut.receiveCatching().onFailure { return@launch }.getOrNull()!!
                    val n = w.now()
                    println("${w.now() / q.toDouble()} + ${n - last} : $q")
                    last = n
                }
            }
            for (i in 0..19) {
                queIn.send(i)
            }
            queIn.close()
        }
    }
}

