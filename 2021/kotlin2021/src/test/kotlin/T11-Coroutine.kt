import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.ExperimentalTime


@ExperimentalTime
@Suppress("NonAsciiCharacters")
class `T11-Coroutine` {
    @Test
    fun t1_非同期実行() = runBlocking {
        stopWatch { w ->
            val job1 = async {
                delay(100) // coroutine負荷
            }
            val job2 = async {
                delay(150)
            }
            println(w.now())
            assert((0..30).contains(w.now()))

            job1.await() // 結果を待つ
            println(w.now())
            assert((100..150).contains(w.now()))

            job2.await() // 結果を待つ(250msはかからない)
            println(w.now())
            assert((150..200).contains(w.now()))
        }
    }

    @Test
    fun t2_ThreadとCoroutine() = runBlocking {
        fun threadBlocker() = Thread.sleep(100)
        suspend fun coroutineLoad() = delay(100)

        // ThreadのブロックとCoroutineのサスペンド、一時停止する点では同じように見えるが
        stopWatch { w ->
            (1..5).map { threadBlocker() }
            println(w.now())
            assert((500..600).contains(w.now()))
        }
        stopWatch { w ->
            (1..5).map { coroutineLoad() }
            println(w.now())
            assert((500..600).contains(w.now()))
        }

        stopWatch { w ->
            // Threadはasyncでブロックし次のasyncを開始しない
            (1..5).map { async { threadBlocker() } }.awaitAll()
            println(w.now())
            assert((500..600).contains(w.now()))
        }
        stopWatch { w ->
            // Coroutineはasyncで一時処理を中断し次のasyncを開始する
            (1..5).map { async { coroutineLoad() } }.awaitAll()
            println(w.now())
            assert((100..200).contains(w.now()))
        }
    }

    @Test
    fun t3_ブロック関数をサスペンドで待つ() {
        fun threadBlocker() = Thread.sleep(100)

        suspend fun threadBlockerToSuspendable() = suspendCoroutine<Unit> { continuation ->
            val r = threadBlocker()
            continuation.resume(r)
        }

        // デフォルトでCoroutineはシングルスレッド(Dispatchers.Main)で実行される
        // 結局Thread Breakerによって全体がブロックされる。
        runBlocking {
            stopWatch { w ->
                (1..2).map { async { threadBlockerToSuspendable() } }.awaitAll()
                println(w.now())
                assert((200..300).contains(w.now()))
            }
        }

        // CoroutineContextをDispatchers.Defaultにすれば、CPUコア数のスレッドで実行される(最小でも2スレッド)
        runBlocking(Dispatchers.Default) {
            stopWatch { w ->
                (1..2).map { async { threadBlockerToSuspendable() } }.awaitAll()
                println(w.now())
                assert((100..200).contains(w.now()))
            }
        }
    }

    @Test
    fun t4_スループットの調整_セマフォ() = runBlocking {
        stopWatch { w ->
            val sem = Semaphore(3)
            (1..5).map {
                sem.acquire()
                async {
                    delay(100)
                    sem.release()
                }
            }.awaitAll()
            println(w.now())
            assert((200..300).contains(w.now())) // 5個の非同期関数を3個まで同時に実行するので
        }
    }

}
