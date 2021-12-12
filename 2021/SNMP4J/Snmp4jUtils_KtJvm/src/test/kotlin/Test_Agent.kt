import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test

class Test_Agent {
    fun now() = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        .run { "%04d%02d%02d.%02d%02d%02dZ".format(year, monthNumber, dayOfMonth, hour, minute, second) }

    @Test
    fun t1() {

    }
}