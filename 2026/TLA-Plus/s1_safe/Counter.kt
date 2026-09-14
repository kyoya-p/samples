import kotlin.concurrent.thread

/**
 * synchronized による排他制御で increment を一度に1スレッドしか
 * 実行できないようにし、レースコンディションを防いだカウンタ。
 */
class SafeCounter {
    private var count: Int = 0
    private val lock = Any()

    fun increment() {
        synchronized(lock) {
            count++
        }
    }

    fun get(): Int = synchronized(lock) { count }
}

fun main() {
    val counter = SafeCounter()
    val threadCount = 10
    val incrementsPerThread = 100_000

    val threads = List(threadCount) {
        thread {
            repeat(incrementsPerThread) {
                counter.increment()
            }
        }
    }
    threads.forEach { it.join() }

    val expected = threadCount * incrementsPerThread
    println("expected = $expected")
    println("actual   = ${counter.get()}")
}
