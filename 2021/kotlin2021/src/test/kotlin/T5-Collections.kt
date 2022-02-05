import org.junit.jupiter.api.Test

@Suppress("NonAsciiCharacters")
class `T5-Collections` {
    @Test
    fun `t1-無限シーケンス`() {
        fun infinit() = sequence { while (true) yield(Unit) }

        infinit().take(10).forEachIndexed { i, e ->
            println(i)
        }
    }

    @Test
    fun `t2-シーケンスの繰り返し`() {
        fun <T> Sequence<T>.repeat(limit: Int) = sequence { repeat(limit) { yieldAll(this@repeat) } }

        sequenceOf(1, 2, 3).repeat(2).forEach { println(it) }
    }
}