import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@ExperimentalTime
class Watch(private val start: Instant = Clock.System.now()) {
    fun now() = (Clock.System.now() - start).inWholeMilliseconds.toInt()
}

@ExperimentalTime
suspend fun <T> stopWatch(op: suspend (Watch) -> T) = op(Watch())

@ExperimentalTime
suspend fun delayUntilNextPeriod(
    interval: Duration,
    now: Instant = Clock.System.now(),
    start: Instant = Instant.fromEpochMilliseconds(0),
): Instant {
    val runTime = when {
        now < start -> start
        else -> start + (interval.inWholeMilliseconds * ((now - start + interval) / interval).roundToLong()).milliseconds
    }
    delay(runTime - now)
    return runTime
}