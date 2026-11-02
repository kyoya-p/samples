import kotlin.concurrent.thread

/**
 * count++ は read-modify-write の3ステップからなり、
 * 複数スレッドから同時に呼ばれるとステップの間に割り込みが発生し、
 * 更新が失われる（lost update）レースコンディションが起きる。
 */
class UnsafeCounter {
    var count: Int = 0

    fun increment() {
        count++
    }
}

fun main() {
    val counter = UnsafeCounter()
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
    println("actual   = ${counter.count}")
    println("lost updates = ${expected - counter.count}")
}
