import kotlin.time.Duration.Companion.milliseconds
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.*
import okio.Path.Companion.toPath

fun main(args: Array<String>) = FileSystem.SYSTEM.run {
    listRecursively(args[0].toPath()).filter { metadata(it).isDirectory }.forEach { f ->
        var tOrg: Instant = Instant.fromEpochSeconds(0)
        fun String.time() = (tOrg + toLong().milliseconds).toLocalDateTime(TimeZone.currentSystemDefault())
        source(f).buffer().use { src ->
            generateSequence { src.readUtf8Line() }.filter { it.contains("ARMM") || it.contains("HTTPCL") }
                .forEach { ln ->
                    Regex("""ARMM,.*,(\d+)# .*\((\d+)\).*(task start|set interval timer)""").find(ln)?.groupValues?.let { m ->
                        tOrg = Instant.fromEpochMilliseconds(m[2].toLong() * 1000 - m[1].toLong())
                    }
                    Regex("""ARMM,.*,(\d+)# .*""").find(ln)?.groupValues?.let { m -> println("${m[1].time()} ${f.name} $ln") }
                    Regex(""".*\[HTTPCL].*""").find(ln)?.let { println("${"0".time()} ${f.name} $ln") }
                }
        }
    }
}

