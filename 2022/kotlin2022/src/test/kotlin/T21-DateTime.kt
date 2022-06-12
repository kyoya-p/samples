import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.*
import kotlinx.datetime.Clock.System.now
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Suppress("NonAsciiCharacters", "ClassName")
// Kotlin 1.6~
class `T21-DateTime` {
    @Test
    fun `t01-時刻`() {
        val t0 = Instant.fromEpochMilliseconds(0)
        val t01 = Instant.parse("1970-01-01T00:00:00Z") //タイムz－ン'Z'は必要

        println(t01)
        assert(t01.toEpochMilliseconds() == 0L)
        assert(t01 == t0)
    }

    @ExperimentalTime
    @Test
    fun `t02-タイムゾーン付き時刻`() {
        // ZoneId定義
        assert(TimeZone.availableZoneIds.contains("Asia/Tokyo"))
        assert(TimeZone.availableZoneIds.contains("Japan"))
        assert(TimeZone.availableZoneIds.contains("Etc/GMT+9"))
        assert(TimeZone.availableZoneIds.contains("Etc/UTC"))

        val t0 = Instant.fromEpochMilliseconds(0)
        val dtUtc: LocalDateTime = t0.toLocalDateTime(TimeZone.of("Etc/UTC"))
        val dtJpn = t0.toLocalDateTime(TimeZone.of("Japan"))

        assert(dtUtc.hour == 0)
        assert(dtJpn.hour == 9)
        assert(dtUtc.toInstant(TimeZone.UTC) - dtJpn.toInstant(TimeZone.of("Japan")) == 0.hours)
    }

    @ExperimentalTime
    @Test
    fun `t03-期間`() {
        val t0 = Instant.fromEpochMilliseconds(0)
        val t1h = Instant.parse("1970-01-01T01:00:00Z")
        val hour1 = 1.hours
        println(t0 + hour1)
        assert(t0 + hour1 == t1h)
        assert(t1h - hour1 == t0)

        fun daysOfFeb(year: Int) = Instant.parse("$year-03-01T00:00:00Z") - Instant.parse("$year-02-01T00:00:00Z")
        assert(daysOfFeb(2000) == 29.days)
        assert(daysOfFeb(2001) == 28.days)
        assert(daysOfFeb(2003) == 28.days)
        assert(daysOfFeb(2004) == 29.days)
        assert(daysOfFeb(2100) == 28.days)
    }

    @Test
    @ExperimentalTime
    fun `t11-周期的な実行1`() = runBlocking {
        suspend fun timer(interval: Duration, start: Instant = now(), op: (scheduled: Instant) -> Unit) {
            while (true) {
                // 実行時刻まで待つ
                val now = now()
                val runTime = when {
                    now < start -> start
                    else -> start + interval * ((now - start + interval) / interval).roundToInt()
                }
                delay(runTime - now)
                op(runTime)
            }
        }
        stopWatch { w ->
            println("${w.now()}")
            val job = launch { timer(100.milliseconds) { println("${w.now()} ${now()}: $it") } }
            delay(600)
            job.cancel()
        }
    }

    @ExperimentalTime
    @Test
    fun `t12-周期的な実行2`() = runBlocking {
        stopWatch { w ->
            for (i in 1..5) {
                delayUntilNextPeriod(100.milliseconds)
                println(w.now())
            }
        }
    }

    @Test
    fun `t31-formatting_成形`() {
        fun now() = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            .run { "%04d%02d%02d.%02d%02d%02dZ".format(year, monthNumber, dayOfMonth, hour, minute, second) }

        assert(now().matches(Regex("""\d{8}\.\d{6}Z""")))
    }
}

