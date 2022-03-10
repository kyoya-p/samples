import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Semaphore
import org.junit.jupiter.api.Test
import java.io.PipedReader
import java.io.PipedWriter
import java.io.PrintWriter
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.ExperimentalTime


@ExperimentalTime
@Suppress("NonAsciiCharacters")
class `T11-Coroutine` {
    @Test
    fun `t01-非同期実行`() = runBlocking {
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
    fun `t02-ThreadとCoroutine`() = runBlocking {
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
    fun `t02a-コルーチンの入力待ち`(): Unit = runBlocking(Dispatchers.Default) { // Coroutine Context注意
        val o = PipedWriter()
        val i = PipedReader()
        i.connect(o)

        launch {
            println("Reader Waiting: ")
            println(i.buffered().readLine()) // 入力等は見落としがち
            println("Reader Done: ")
        }
        launch {
            println("Writer Pause:")
            delay(100)
            PrintWriter(o).println("aaa")
            println("Writer Done:")
        }
    }

    @Test
    fun `t03-ブロッキング関数をサスペンドに変換`() {
        fun threadBlocker() = Thread.sleep(100)

        suspend fun threadBlockerToSuspendable() = suspendCoroutine<Unit> { continuation ->
            val r = threadBlocker()
            continuation.resume(r)
        }

        // デフォルトでCoroutineはシングルスレッド(Dispatchers.Main)で実行される
        // 結局Thread Blockerによって全体がブロックされる。
        runBlocking {
            stopWatch { w ->
                (1..2).map { async { threadBlockerToSuspendable() } }.awaitAll()
                println(w.now())
                assert((200..300).contains(w.now())) // 200ms必要
            }
        }

        // CoroutineContextをDispatchers.Defaultにすれば、CPUコア数のスレッドで実行される(最小でも2スレッド)
        runBlocking(Dispatchers.Default) {
            stopWatch { w ->
                (1..2).map { async { threadBlockerToSuspendable() } }.awaitAll()
                println(w.now())
                assert((100..200).contains(w.now())) // 100msで終わる
            }
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `t04-コールバック関数をサスペンドに変換`() {
        fun callbackWithAnotherThread(callback: (Int) -> Unit) {
            thread {
                Thread.sleep(100)
                callback(1)
                Thread.sleep(100)
                callback(2)
            }
        }

        suspend fun collbackWithCoroutine(callback: suspend (Int) -> Unit) = callbackFlow {
            callbackWithAnotherThread {
                trySend(it)
                if (it == 2) close() // 終了したい場合
            }
            awaitClose()
        }.collectLatest {
            callback(it)
        }

        runBlocking() {
            stopWatch { w ->
                launch {
                    collbackWithCoroutine {
                        println("${w.now()} $it")
                    }
                }
                collbackWithCoroutine {
                    println("${w.now()} $it")
                }
            }

        }
    }

    @Test
    fun `t04-スループットの調整_セマフォ`() = runBlocking {
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
            assert((200..300).contains(w.now())) // 5個の非同期関数を3個まで同時に実行するので200msぐらい
        }
    }

}
