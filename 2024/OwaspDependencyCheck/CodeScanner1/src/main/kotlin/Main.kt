import kotlinx.serialization.json.Json
import java.io.File

fun main(args: Array<String>) {
    val fileName = args.getOrNull(0) ?: "build/reports/bom.json"
    val json = Json { ignoreUnknownKeys = false }
    val bom: Bom = json.decodeFromString(File(fileName).readText())
    println(bom.components.map{it.name})
}