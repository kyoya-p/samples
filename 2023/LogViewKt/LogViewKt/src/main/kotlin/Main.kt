import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

fun main() = File("samples").walk().filter { it.isFile }.forEach { f ->
    var tOrg: Instant = Instant.fromEpochSeconds(0)
    sequence { f.useLines { it.forEach { yield(it) } } }.forEach {
        Regex("""ARMM,.*,(\d+)# .*\((\d+)\).*(task start|set interval timer)""").find(it)?.let {
            tOrg = Instant.fromEpochMilliseconds(it.groupValues[2].toLong() * 1000 - it.groupValues[1].toLong())
        }
        Regex("""ARMM,.*,(\d+)# .*""").find(it)?.let { m ->
            println("${(tOrg + m.groupValues[1].toLong().milliseconds).toLocalDateTime(TimeZone.currentSystemDefault())} ${f.name} $it")
        }
        Regex(""".*\[HTTPCL]\.*(Requestline|ResponseLine).*""").find(it)?.let { _ -> println(it) }
    }
}
