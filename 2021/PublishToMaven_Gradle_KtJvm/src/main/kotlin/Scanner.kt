import kotlinx.coroutines.delay
import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@ExperimentalTime
suspend fun main() {
    repeat(3) { i ->  // 将来を起点とするならその時刻まで待つ
        val now = now()
        val future = now + 33.milliseconds * i
        val lap = delayUntilNextPeriod(200.milliseconds, now, future)

        assert((-30.milliseconds..30.milliseconds).contains(lap - now()))
        println(lap - now()) //誤差
        assert(lap == future)
    }
    repeat(3) {   // エポックを起点とし指定間隔まで待つ
        val start = now()
        val lap = delayUntilNextPeriod(200.milliseconds, start)
        assert((-30.milliseconds..30.milliseconds).contains(lap - now()))
        println(lap - now()) //誤差
        assert(lap == start)
    }

    val start = now()
    var lap = start
    repeat(3) { i ->   // 指定時刻を起点とし指定間隔まで繰り返し待つ
        val r = delayUntilNextPeriod(200.milliseconds, origin = start)
        assert((-30.milliseconds..30.milliseconds).contains(start + 200.milliseconds * i - now()))
        assert(r == start)
        delay(1)
        println("lap=${lap - start}")
        lap = now()
    }
}

private fun Long.ceilDiv(step: Long) = -((-this).floorDiv(step))
private fun step(x: Long, step: Long, origin: Long) = (x - origin).ceilDiv(step) * step + origin

@ExperimentalTime
suspend fun delayUntilNextPeriod(
    interval: Duration,
    now: Instant = now(),
    origin: Instant = Instant.fromEpochMilliseconds(0),
): Instant {

    val runTime = Instant.fromEpochMilliseconds(when {
        now <= origin -> origin.toEpochMilliseconds()
        else -> step(now.toEpochMilliseconds(), interval.inWholeMilliseconds, origin.toEpochMilliseconds())
    })
    delay(runTime - now)
    return runTime
}