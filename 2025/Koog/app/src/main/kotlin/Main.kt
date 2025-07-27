import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

fun main() {
    val r = generateSequence { readlnOrNull() }
        .mapNotNull { Regex("StatusReport\\[(.*)]").find(it)?.groupValues?.getOrNull(1) }
        .map { row ->
            println(row)
            runCatching {
                Json {}.parseToJsonElement(row).jsonObject.also(::println)
            }.onFailure {
                println(it)
            }.getOrThrow()
        }
}
