@file:Suppress("UseExpressionBody")

import kotlinx.datetime.*
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import kotlin.time.Duration.Companion.milliseconds


@Suppress("ClassName", "NonAsciiCharacters")
class V1_7_20 {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    // -language-version 1.8
    fun 半開区間演算子Int() {
        val 閉区間 = 1..4
        val 半開区間 = 1 ..< 4
        val until区間 = 1 until 4
        閉区間.forEach { print(it) }; println()
        半開区間.forEach { print(it) }; println()
        until区間.forEach { print(it) }; println()
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    // -language-version 1.8
    fun 半開区間演算子Double() {
        val 閉区間 = 1.0..4.0
        val 半開区間 = 1.0 ..< 4.0
        // val until区間 = (1.0 until 4.0) // error
        (38..42).map { it.toFloat() / 10 }.filter { it in 閉区間 }.joinToString(",").run(::println)
        (38..42).map { it.toFloat() / 10 }.filter { it in 半開区間 }.joinToString(",").run(::println)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    // -language-version 1.8
    fun 半開区間演算子Date() {
        val apr = LocalDateTime.parse("2022-04-01T00:00:00") ..< LocalDateTime.parse("2022-05-01T00:00:00")
        for (i in 998..1002) {
            val time = LocalDateTime.parse("2022-04-30T23:59:59").toInstant(TimeZone.UTC) + i.milliseconds
            val localTime = time.toLocalDateTime(TimeZone.UTC)
            println("$localTime:${localTime in apr}")
        }
    }

    // ---------------------
    @Test
    // -language-version 1.9
    fun sealedClass() {
        fun BufferedReader.readTest(): ReadResult {
            val r = readLine() ?: return ReadResult.EndOfFile
            runCatching { return ReadResult.Number(r.toInt()) }
            return ReadResult.Text(r)
        }

        val s = "123\nabc".reader().buffered()
        while (true) {
            when (val r = s.readTest()) { // 選択肢はもれなく
                ReadResult.EndOfFile -> break
                is ReadResult.Number -> println(r) // 見やすい表示 Number(value=123)
                is ReadResult.Text -> println(r) // 普通の表示 V1_7_20$ReadResult$Text@4f49....
            }
        }
    }

    sealed class ReadResult {
        data class Number(val value: Int) : ReadResult()
        class Text(val value: String) : ReadResult()
        data object EndOfFile : ReadResult()
    }

    // ---------------------
    @Test
    fun inlineClass() {
        val i = num<Int>(1)
        println(i)
        println(i.javaClass)
        println(i.hashCode())
        println(1)
        println(1.javaClass)
        println(1.hashCode())
        fun a(v: Any) {
            when (v) {
                is num<*> -> println("num<*>/${v.javaClass}") // inlineなの?
                is Int -> println("Int/${v.javaClass}")
                else -> println("else/${v.javaClass}")
            }
        }
        a(num(1))
    }

    @JvmInline
    value class num<T>(val value: T)
}


