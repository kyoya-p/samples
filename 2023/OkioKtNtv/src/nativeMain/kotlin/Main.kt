import kotlin.time.Duration.Companion.milliseconds
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.*
import okio.Path.Companion.toPath

fun Path.walk(): Sequence<Path> = when {
    FileSystem.SYSTEM.metadata(this).isDirectory -> FileSystem.SYSTEM.list(this).asSequence().flatMap { it.walk() }
    else -> sequenceOf(this)
}

fun main(args: Array<String>) = args[0].toPath().walk().forEach { f ->
    var tOrg: Instant = Instant.fromEpochSeconds(0)
    FileSystem.SYSTEM.source(f).buffer().use {
        generateSequence { it.readUtf8Line() }.forEach {
            Regex("""ARMM,.*,(\d+)# .*\((\d+)\).*(task start|set interval timer)""").find(it)?.let {
                tOrg = Instant.fromEpochMilliseconds(it.groupValues[2].toLong() * 1000 - it.groupValues[1].toLong())
            }
            Regex("""ARMM,.*,(\d+)# .*""").find(it)?.let { m ->
                println("${(tOrg + m.groupValues[1].toLong().milliseconds).toLocalDateTime(TimeZone.currentSystemDefault())} ${f.name} $it")
            }
            Regex(""".*\[HTTPCL].*(Requestline|ResponseLine).*""").find(it)?.let { _ -> println(it) }
        }
    }
}
