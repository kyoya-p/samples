import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.ExperimentalTime

@ExperimentalTime
class Watch(private val start: Instant = Clock.System.now()) {
    fun now() = (Clock.System.now() - start).inWholeMilliseconds.toInt()
}

@ExperimentalTime
suspend fun <T> stopWatch(op: suspend (Watch) -> T) = op(Watch())
