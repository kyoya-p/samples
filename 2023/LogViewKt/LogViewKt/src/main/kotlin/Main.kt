import kotlinx.datetime.Instant
import java.io.File

fun main() = File("samples").walk().filter { it.isFile }.forEach { f ->
    var tStart: Instant = Instant.parse("1970-01-01T00:00:00Z")
    sequence { f.useLines { it.forEach { yield(it) } } }.filter { it.contains("ARMM") }.forEach {
        when {
            it.contains("set interval timer") || it.contains("task start") -> {
                val t = Regex("""\((\d+)\)""").find(it)?.groupValues?.get(1)?.toLong()
                if (t != null) tStart = Instant.fromEpochSeconds(t)
                println("${tStart}   --- $it")
            }
        }
        println("${tStart} ${f.name} $it")
    }
}