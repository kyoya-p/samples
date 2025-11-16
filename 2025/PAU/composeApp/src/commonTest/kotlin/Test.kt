import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.matchers.ranges.shouldBeInOpenEndRange
import io.kotest.matchers.shouldBe
import jp.wjg.shokkaa.snmp.RateLimiter
import jp.wjg.shokkaa.snmp.ULongRangeSet
import jp.wjg.shokkaa.snmp.asFlatSequence
import jp.wjg.shokkaa.snmp.rateLimited
import jp.wjg.shokkaa.snmp.toFlatFlow
import jp.wjg.shokkaa.snmp.toIpV4ULong
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import java.net.UnknownHostException
import kotlin.time.Clock.System.now
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class, ExperimentalStdlibApi::class)
class MyTest : FunSpec({
    test("rateLimiter:1/100ms:10 実行時刻待ち") {
        fun Instant.lap() = now() - this
        run {
            val s = now()
            (0..<10).asFlow().collect {
                s.lap() shouldBeLessThan 50.milliseconds
            }
            s.lap() shouldBeLessThan 50.milliseconds
        }

        val m = 150.milliseconds
        measureTime {
            val s = now()
            val limiter100ms = RateLimiter(100.milliseconds, s)
            (0..<10).asFlow().collect { ix ->
                limiter100ms.runRateLimited {
                    s.lap() shouldBeIn (100.milliseconds * ix - m..100.milliseconds * ix + m)
                }
            }
        } shouldBeIn (1000.milliseconds - m..1000.milliseconds + m)

    }
    test("rateLimiter:一つのLimitorを複数のコンテキストで使用") {
        measureTime {
            val s = now()
            (0..<10).asFlow().collect { println("${now() - s}") }
        } shouldBeLessThan 10.milliseconds
        measureTime {
            val s = now()
            (0..<10).asFlow().rateLimited(RateLimiter(100.milliseconds)).collect { println("${now() - s}") }
        } shouldBeInOpenEndRange 900.milliseconds..<1100.milliseconds

        val rl = RateLimiter(20.milliseconds)
        measureTime {
            val s = now()
            (0..<50).asFlow().rateLimited(rl).collect { println("${now() - s}") }
        } shouldBeInOpenEndRange 900.milliseconds..<1100.milliseconds
    }

    // 非同期
    test("rateLimiter3") {
        val rl = RateLimiter(20.milliseconds)
        delay(500.milliseconds)
        measureTime {
            val s = now()
            (0..<50).asFlow().rateLimited(rl).collect { println("${now() - s}") }
        } shouldBeInOpenEndRange 900.milliseconds..<1100.milliseconds
    }

    test("toRangeSet1-0") {
        shouldThrow<Exception> { "".toIpV4ULong() }
        shouldThrow<UnknownHostException> { "a".toIpV4ULong() }

        "".toRangeSet().asFlatSequence().toList() shouldBe listOf()
        "0".toRangeSet().asFlatSequence().toList() shouldBe listOf("0.0.0.0".toIpV4ULong()) // no class
        "1.2".toRangeSet().asFlatSequence().toList() shouldBe listOf("1.0.0.2".toIpV4ULong()) // class A
        "1.65536".toRangeSet().asFlatSequence().toList() shouldBe listOf("1.1.0.0".toIpV4ULong()) // class A
        "1.2.3".toRangeSet().asFlatSequence().toList() shouldBe listOf("1.2.0.3".toIpV4ULong()) // class B
        "1.2.256".toRangeSet().asFlatSequence().toList() shouldBe listOf("1.2.1.0".toIpV4ULong()) // class B
        "1.2.3.4".toRangeSet().asFlatSequence().toList() shouldBe listOf("1.2.3.4".toIpV4ULong()) // class C
        "65536".toRangeSet().asFlatSequence().toList() shouldBe listOf("0.1.0.0".toIpV4ULong()) // no class
    }
    test("toRangeSet1-1") {
        fun ULongRangeSet.toULongList() = asFlatSequence().toList()

        val r1 = 1UL..1UL
        val r3 = 3UL..3UL

        ULongRangeSet().asFlatSequence().toList() shouldBe emptyList()
        ULongRangeSet(r1).asFlatSequence().toList() shouldBe listOf(1UL)
        ULongRangeSet(listOf(r1)).asFlatSequence().toList() shouldBe listOf(1UL)
        ULongRangeSet(listOf(r1, r1)).asFlatSequence().toList() shouldBe listOf(1UL)

        val rs0 = ULongRangeSet()
        rs0.asFlatSequence().toList() shouldBe emptyList()
        rs0.add(r1)
        rs0.asFlatSequence().toList() shouldBe listOf(1UL)
        rs0.add(r1)
        rs0.asFlatSequence().toList() shouldBe listOf(1UL)

        val rs1 = ULongRangeSet(r1)
        rs1.asFlatSequence().toList() shouldBe listOf(1UL)
        rs1.addAll(listOf(r1))
        rs1.asFlatSequence().toList() shouldBe listOf(1UL)

        fun String.toRangeSet() =
            ULongRangeSet(split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { it.toRange() })

        val x = ULongRangeSet(listOf(0UL..1UL, 0UL..1UL))
        println(x.toList())
        val y = ULongRangeSet().apply { addAll(listOf(0UL..0UL, 0UL..0UL)) }
        println(y.toList())
    }

    test("toRangeSet1-2") {
        "".toRangeSet().asFlatSequence().toList() shouldBe listOf()
        "0".toRangeSet().asFlatSequence().toList() shouldBe listOf(0UL)
        "0,0".toRangeSet().asFlatSequence().toList() shouldBe listOf(0UL)
        "0,2".toRangeSet().asFlatSequence().toList() shouldBe listOf(0UL, 2UL)
        "0,1,255".toRangeSet().asFlatSequence().toList() shouldBe listOf(0UL, 1UL, 255UL)
        "1,,2".toRangeSet().asFlatSequence().toList() shouldBe listOf(1UL, 2UL)
        ",,1,,,,2,".toRangeSet().asFlatSequence().toList() shouldBe listOf(1UL, 2UL)
        "4,2,6".toRangeSet().asFlatSequence().toList() shouldBe listOf(2UL, 4UL, 6UL)

    }
    test("toRangeSet1-3") {
        "".toRangeSet().asFlatSequence().toList() shouldBe listOf()
        "1-3".toRangeSet().asFlatSequence().toList() shouldBe listOf(1UL, 2UL, 3UL)
        "1-1".toRangeSet().asFlatSequence().toList() shouldBe listOf(1UL)
        "3-1".toRangeSet().asFlatSequence().toList() shouldBe listOf() // reversed is empty
        "1-3,5-7".toRangeSet().asFlatSequence().toList() shouldBe listOf(1UL, 2UL, 3UL, 5UL, 6UL, 7UL)
        "1-3,4-5".toRangeSet() shouldBe "1-5".toRangeSet()
        "1-3,4,5,6-7,0".toRangeSet() shouldBe "0-7".toRangeSet()

        "0,1-3,0.0.1.1-0.0.1.3".toRangeSet().asFlatSequence().toList() shouldBe listOf(
            0UL,
            1UL,
            2UL,
            3UL,
            "0.0.1.1".toIpV4ULong(),
            "0.0.1.2".toIpV4ULong(),
            "0.0.1.3".toIpV4ULong()
        )
    }
    test("slicedFlow2") {
        "0,1-3,0.0.1.1-0.0.1.3".toRangeSet().asFlatSequence().toList() shouldBe listOf(
            0UL,
            1UL,
            2UL,
            3UL,
            "0.0.1.1".toIpV4ULong(),
            "0.0.1.2".toIpV4ULong(),
            "0.0.1.3".toIpV4ULong()
        )

        val src = "0,1-3,0.0.1.1-0.0.1.3".toRangeSet()
        println(src.toList())

        src.asFlatSequence().toList().also(::println) shouldBe listOf(
            0UL,
            1UL,
            2UL,
            3UL,
            "0.0.1.1".toIpV4ULong(),
            "0.0.1.2".toIpV4ULong(),
            "0.0.1.3".toIpV4ULong(),
        )
        src.toFlatFlow(2UL).toList().also(::println) shouldBe listOf(
            "0",
            "2",
            "0.0.1.2",
            "1",
            "3",
            "0.0.1.1",
            "0.0.1.3"
        ).map { it.toIpV4ULong() }
        src.toFlatFlow(3UL).toList().also(::println) shouldBe listOf(
            "0",
            "2",
            "0.0.1.2",
            "1",
            "3",
            "0.0.1.1",
            "0.0.1.3"
        ).map { it.toIpV4ULong() }
    }
})
