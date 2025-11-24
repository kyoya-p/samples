import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.matchers.ranges.shouldBeInOpenEndRange
import io.kotest.matchers.shouldBe
import jp.wjg.shokkaa.snmp.RateLimiter
import jp.wjg.shokkaa.snmp.asFlatSequence
import jp.wjg.shokkaa.snmp.rateLimited
import jp.wjg.shokkaa.snmp.toIpV4RangeSet
import jp.wjg.shokkaa.snmp.toIpV4ULong
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
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
})
