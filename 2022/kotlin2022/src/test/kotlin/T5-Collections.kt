import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

@Suppress("NonAsciiCharacters")
class `T5-Collections` {
    @Test
    fun `t1-無限シーケンス`() {
        fun infinit() = sequence { while (true) yield(Unit) }

        infinit().take(10).forEachIndexed { i, _ ->
            println(i)
        }
    }

    @Test
    fun `t2-シーケンスの繰り返し`() {
        fun <T> Sequence<T>.repeat(limit: Int) = sequence { repeat(limit) { yieldAll(this@repeat) } }

        sequenceOf(1, 2, 3).repeat(2).forEach { println(it) }
    }

    @ExperimentalSerializationApi
    @Test
    fun `t3-Json化`() {
        val json = Json

        assert(json.decodeFromString<List<Int>>("[1,5,2,4]") == listOf(1, 5, 2, 4))
        assert(json.encodeToString(listOf(5, 4, 1)) == "[5,4,1]")
        assert(json.decodeFromStream<List<Int>>("[1,5,2,4]".byteInputStream()) == listOf(1, 5, 2, 4))

        val byteArrayStream = ByteArrayOutputStream()
        json.encodeToStream(listOf(5, 4, 1), byteArrayStream)
        assert(byteArrayStream.toString() == "[5,4,1]")
    }
}