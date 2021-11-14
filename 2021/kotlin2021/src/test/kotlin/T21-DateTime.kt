import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.*
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@Suppress("NonAsciiCharacters")
class `T21-DateTime` {
    @Test
    fun `t1-時刻`() {
        val t0 = Instant.fromEpochMilliseconds(0)
        val t01 = Instant.parse("1970-01-01T00:00:00Z") //タイムz－ン'Z'は必要

        println(t01)
        assert(t01.toEpochMilliseconds() == 0L)
        assert(t01 == t0)
    }

    @ExperimentalTime
    @Test
    fun `t2-タイムゾーン付き時刻`() {
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
        assert(dtUtc.toInstant(TimeZone.UTC) - dtJpn.toInstant(TimeZone.of("Japan")) == Duration.hours(0))
    }

    @ExperimentalTime
    @Test
    fun `t3-期間`() {
        val t0 = Instant.fromEpochMilliseconds(0)
        val t1h = Instant.parse("1970-01-01T01:00:00Z")
        val hour1 = Duration.hours(1)
        println(t0 + hour1)
        assert(t0 + hour1 == t1h)
        assert(t1h - hour1 == t0)

        fun daysOfFeb(year: Int) = Instant.parse("$year-03-01T00:00:00Z") - Instant.parse("$year-02-01T00:00:00Z")
        assert(daysOfFeb(2000) == Duration.days(29))
        assert(daysOfFeb(2001) == Duration.days(28))
        assert(daysOfFeb(2003) == Duration.days(28))
        assert(daysOfFeb(2004) == Duration.days(29))
        assert(daysOfFeb(2100) == Duration.days(28))
    }

    @Test
    @ExperimentalTime
    fun `t11-周期的な実行`() = runBlocking {
        suspend fun timer(interval: Duration, start: Instant = Clock.System.now(), op: (scheduled: Instant) -> Unit) {
            while (true) {
                // 実行時刻まで待つ
                val now = Clock.System.now()
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
            val job = launch { timer(Duration.milliseconds(100)) { println("${w.now()} ${Clock.System.now()}: $it") } }
            delay(600)
            job.cancel()
        }
    }
}
