import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.ranges.shouldBeInOpenEndRange
import io.kotest.matchers.shouldBe
import jp.wjg.shokkaa.snmp.RateLimiter
import jp.wjg.shokkaa.snmp.asFlatSequence
import jp.wjg.shokkaa.snmp.rateLimited
import jp.wjg.shokkaa.snmp.toFlatFlow
import jp.wjg.shokkaa.snmp.toIpV4ULong
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlin.time.Clock.System.now
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class, ExperimentalStdlibApi::class)
class MyTest : FunSpec({
    test("rateLimiter") {
        fun src(n: Int, s: Instant = now()) = (0..<n).asFlow().map { now() - s }
        src(10).collectIndexed { i, e ->
            println("$i: $e")
            assert(e < 50.milliseconds)
        }

        val limiter100ms = RateLimiter(100.milliseconds)
        src(10).collectIndexed { i, e ->
            limiter100ms.runRateLimited {
                println("$i: $e")
                assert(100.milliseconds * i < e)
                assert(e < 100.milliseconds * (i + 1))
            }
        }
        val limiter1s = RateLimiter(1000.milliseconds)
        src(10).collectIndexed { i, e ->
            limiter1s.runRateLimited {
                println("$i: $e")
                assert(1.seconds * i < e)
                assert(e < 1.seconds * (i + 1))
            }
        }
    }
    test("rateLimiter2") {
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
    // 実行できなければスキップ
    test("rateLimiter3") {
        val rl = RateLimiter(20.milliseconds)
        delay(500.milliseconds)
        measureTime {
            val s = now()
            (0..<50).asFlow().rateLimited(rl).collect { println("${now() - s}") }
        } shouldBeInOpenEndRange 900.milliseconds..<1100.milliseconds
    }

    test("slicedSequence") {
        val src = "0,1-3,0.0.1.1-0.0.1.3".toRangeSet()
        val adrList = listOf("0", "1", "2", "3", "0.0.1.1", "0.0.1.2", "0.0.1.3")

        src.asFlatSequence().toList().also(::println)
    }
    test("slicedFlow") {
        val src = "0,1-3,0.0.1.1-0.0.1.3".toRangeSet()

        src.toFlatFlow().toList().also(::println) shouldBe listOf(
            "0",
            "1",
            "2",
            "3",
            "0.0.1.1",
            "0.0.1.2",
            "0.0.1.3"
        ).map { it.toIpV4ULong() }
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

