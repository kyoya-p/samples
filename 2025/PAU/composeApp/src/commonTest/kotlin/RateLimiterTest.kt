import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.matchers.ranges.shouldBeInOpenEndRange
import jp.wjg.shokkaa.snmp.RateLimiter
import jp.wjg.shokkaa.snmp.rateLimited
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.time.Clock.System.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class, ExperimentalStdlibApi::class)
class RateLimiterTest : FunSpec({

    fun Instant.lap() = now() - this
    fun around(v: Duration, mergin: Double = 1.2): ClosedRange<Duration> = v / mergin..v * mergin


    test("1 limiter x 1 source") {
        val limiter = RateLimiter(100.milliseconds, 1)
        val ts = now()
        val src = flow { repeat(10) { emit(ts.lap()) }; delay(100) }// 100msに一回時刻を生成
        measureTime {
            src.rateLimited(limiter).collectIndexed { i, e -> println("$i: $e") }
        } shouldBeIn around(1000.milliseconds)
    }

    test("1 limiter x 2 source") {
        val limiter = RateLimiter(100.milliseconds, 1)
        val ts = now()
        val src = flow { repeat(10) { emit(ts.lap()) }; delay(100.milliseconds) }
        measureTime {
            val j1 = launch { src.rateLimited(limiter).collectIndexed { i, e -> println("$i: $e") } }
            val j2 = launch { src.rateLimited(limiter).collectIndexed { i, e -> println("$i: $e") } }
            joinAll(j1, j2)
        } shouldBeIn around(2000.milliseconds)
    }

    test("2 limiter x 1 source") {
        val limiter1 = RateLimiter(100.milliseconds, 1)
        val limiter2 = RateLimiter(100.milliseconds, 1)
        val ts = now()
        val src = flow { repeat(10) { emit(ts.lap()) }; delay(100.milliseconds) }
        measureTime {
            src.rateLimited(limiter1).rateLimited(limiter2).collectIndexed { i, e -> println("$i: $e") }
        } shouldBeIn around(2000.milliseconds)
    }

    test("2 limiter x 2 source") {
        val limiter1 = RateLimiter(100.milliseconds, 1)
        val limiter2 = RateLimiter(100.milliseconds, 1)
        val ts = now()
        val src = flow { repeat(10) { emit(ts.lap()) }; delay(100.milliseconds) }
        measureTime {
            val j1 = launch { src.rateLimited(limiter1).rateLimited(limiter2).collectIndexed { i, e -> println("$i: $e") } }
            val j2 = launch { src.rateLimited(limiter1).rateLimited(limiter2).collectIndexed { i, e -> println("$i: $e") } }
            joinAll(j1, j2)
        } shouldBeIn around(4000.milliseconds)
    }

    test("burst test") {
        val limiter = RateLimiter(100.milliseconds, 10)
        val ts = now()
        val src = flow { repeat(100) { emit(ts.lap()) }; delay(100.milliseconds) }
        measureTime {
            src.rateLimited(limiter).collectIndexed { i, e ->
                if (i < 10) e.shouldBeLessThan(100.milliseconds)
                println("$i: $e")
            }
        } shouldBeIn around(1000.milliseconds)
    }

    test("burst test with 2 sources") {
        val limiter = RateLimiter(100.milliseconds, 10)
        val ts = now()
        val src = flow { repeat(100) { emit(ts.lap()) }; delay(100.milliseconds) }
        measureTime {
            val j1 = launch { src.rateLimited(limiter).collectIndexed { i, e -> if (i < 10) e.shouldBeLessThan(100.milliseconds) else e.shouldBeGreaterThan(100.milliseconds); println("j1 $i: $e") } }
            val j2 = launch { src.rateLimited(limiter).collectIndexed { i, e -> if (i < 10) e.shouldBeLessThan(100.milliseconds) else e.shouldBeGreaterThan(100.milliseconds); println("j2 $i: $e") } }
            joinAll(j1, j2)
        } shouldBeIn around(2000.milliseconds)
    }

    test("burst test with 2 sources and 2 limiters") {
        val limiter1 = RateLimiter(100.milliseconds, 10)
        val limiter2 = RateLimiter(100.milliseconds, 10)
        val ts = now()
        val src = flow { repeat(100) { emit(ts.lap()) }; delay(100.milliseconds) }
        measureTime {
            val j1 = launch { src.rateLimited(limiter1).rateLimited(limiter2).collectIndexed { i, e -> if (i < 10) e.shouldBeLessThan(100.milliseconds) else e.shouldBeGreaterThan(100.milliseconds); println("j1 $i: $e") } }
            val j2 = launch { src.rateLimited(limiter1).rateLimited(limiter2).collectIndexed { i, e -> if (i < 10) e.shouldBeLessThan(100.milliseconds) else e.shouldBeGreaterThan(100.milliseconds); println("j2 $i: $e") } }
            joinAll(j1, j2)
        } shouldBeIn around(2000.milliseconds)
    }


})
