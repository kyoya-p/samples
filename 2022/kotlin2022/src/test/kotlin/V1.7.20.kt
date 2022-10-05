@file:Suppress("UNUSED_VARIABLE", "UseExpressionBody")

import kotlinx.datetime.*
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds


@Suppress("ClassName", "NonAsciiCharacters", "TestFunctionName")
class V1_7_20 {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `半開区間演算子Int`() {
        val 閉区間 = 1..4
        val 半開区間 = 1 ..< 4
        val until区間 = 1 until 4
        閉区間.forEach { print(it) }; println()
        半開区間.forEach { print(it) }; println()
        until区間.forEach { print(it) }; println()
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `半開区間演算子Double`() {
        val 閉区間 = 1.0..4.0
        val 半開区間 = 1.0 ..< 4.0
        // val until区間 = (1.0 until 4.0) // error
        (38..42).map { it.toFloat() / 10 }.filter { it in 閉区間 }.joinToString(",").run(::println)
        (38..42).map { it.toFloat() / 10 }.filter { it in 半開区間 }.joinToString(",").run(::println)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `半開区間演算子Date`() {
        val apr = LocalDateTime.parse("2022-04-01T00:00:00") ..< LocalDateTime.parse("2022-05-01T00:00:00")
        for (i in 998..1002) {
            val time = LocalDateTime.parse("2022-04-30T23:59:59").toInstant(TimeZone.UTC) + i.milliseconds
            val localTime = time.toLocalDateTime(TimeZone.UTC)
            println("$localTime:${localTime in apr}")
        }
    }

    sealed class ReadResult {
        data class Number(val value: Int) : ReadResult()
        data class Text(val value: String) : ReadResult()
        data object EndOfFile : ReadResult()
    }

    @Test
    fun sealedClass() {
        fun aFunction(): ReadResult {
            return ReadResult.EndOfFile
        }
        when (val r = aFunction()) {
            ReadResult.EndOfFile -> println(r) // data object 読みやすい, sealed class なので記述漏れない
            is ReadResult.Number -> println(r) // data class 読みやすい, sealed class なので記述漏れない
            is ReadResult.Text -> println(r) // data class 読みやすい, sealed class なので記述漏れない
        }
    }
}


