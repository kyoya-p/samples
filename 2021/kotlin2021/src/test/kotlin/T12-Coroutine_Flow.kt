import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Suppress("NonAsciiCharacters")
class `T12-Coroutine_Flow` {

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
    // シンクの処理が遅くても、バッファを設定することでソースだけは高速で出力できる
    fun t5_スループット_buffer() = runBlocking {
        stopWatch { w ->
            flow {
                for (i in 0 until 5) {
                    assert((0..100).contains(w.now()))
                    emit(i)
                }
            }.buffer(5).collect { i ->
                assert(((i * 100)..(i * 100 + 100)).contains(w.now()))
                delay(100)
            }

            println(w.now())
            assert((500..600).contains(w.now()))
        }
    }
}