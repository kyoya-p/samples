import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Suppress("NonAsciiCharacters", "TestFunctionName", "ClassName")
class `T16-Coroutine_Flow` {

    @Test
    fun t1_flow() = runBlocking {
        val f5 = flow {
            for (i in 1..5) {
                emit(i)
            }
        }
        f5.collect { println(it) }
    }

    @Test
    fun t2_flow() = runBlocking {
        val f5 = (1..5).asFlow()
        f5.collect { println(it) }
    }

    @Test
    fun t3_ソースのスループット() = runBlocking {
        stopWatch { w ->
            flow {
                for (i in 0..4) {
                    assert(((i * 100)..(i * 100 + 100)).contains(w.now()))
                    emit(i)
                    delay(100)
                }
            }.collect { i ->
                assert(((i * 100)..(i * 100 + 100)).contains(w.now()))
            }
        }
    }

    @Test
    // シンクの処理が遅い場合、ソースの処理も遅くなる
    // 基本的には、全体の処理時間はソースの処理時間+シンクの処理時間
    fun t4_シンクのスループット() = runBlocking {
        stopWatch { w ->
            flow {
                for (i in 0 until 5) {
                    assert(((i * 100 * 2)..(i * 100 * 2 + 100)).contains(w.now()))
                    emit(i)
                    delay(100)
                }
            }.collect { i ->
                assert(((i * 100 * 2)..(i * 100 * 2 + 100)).contains(w.now()))
                delay(100)
            }
            println(w.now())
            assert((1000..1200).contains(w.now()))
        }
    }

    @Test
    @Suppress("unused")
    // シンクの処理が遅くても、バッファを設定することでソースだけは高速で出力できる
    fun t5_スループット_buffer() = runBlocking {
        stopWatch { w ->
            flow {
                for (i in 0 until 5) {
                    assert((0..100).contains(w.now()))
                    emit(i)
                }
            }.buffer().collect { i ->
                assert(((i * 100)..(i * 100 + 100)).contains(w.now()))
                delay(100)
            }

            println(w.now())
            assert((500..600).contains(w.now()))
        }

        // キュー可能なメッセージ数を整数で指定する以外にいくつかの指定方法がある
        fun <T> Flow<T>.buffer_UNLIMITED() = buffer(Channel.UNLIMITED)  //無制限(Int.MAX_VALUE)
        fun <T> Flow<T>.buffer_RENDEZVOUS() = buffer(Channel.RENDEZVOUS)  //バッファ無し(0)
        fun <T> Flow<T>.buffer_CONFLATED() = buffer(Channel.CONFLATED)  // シンクの処理が間に合わないメッセージは破棄(上書き)
        fun <T> Flow<T>.buffer_BUFFERED() = buffer(Channel.BUFFERED)  // デフォルト(suspendなら64、非suspendなら1)
    }
}