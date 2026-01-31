import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlinx.serialization.decodeFromString

import com.github.ajalt.clikt.core.Context

class GenerateCypher : CliktCommand(name = "generate-cypher") {
    override fun help(context: Context) = "Neo4j Cypher Query Generator"
    
    private val json = Json { ignoreUnknownKeys = true }
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    init {
        context {
            helpOptionNames = setOf("-h", "--help")
            helpFormatter = { context -> MordantHelpFormatter(context, showDefaultValues = true) }
        }
    }

    val cacheDir by option("-d", "--cache-dir", help = "Input directory containing YAML card files")
        .convert { Path(it) }.default(defaultCachePath)

    val output by option("-o", "--output", help = "Output file path (optional, default prints to stdout)")
        .convert { Path(it) }

    val idFilter by option("--id", help = "対象のカードID(部分一致)")

    override fun run() {
        if (!SystemFileSystem.exists(cacheDir)) {
            echo("Error: Cache directory not found: $cacheDir", err = true)
            return
        }

        val generator = CypherGenerator()
        val sb = StringBuilder()
        
        // Header for the Cypher script
        sb.append("// Generated Cypher Query for Battle Spirits Cards\n")
        sb.append("// Generated at: ").append(getCurrentTimestamp()).append("\n\n")

        val targetDir = if (SystemFileSystem.exists(Path(cacheDir, "yaml"))) Path(cacheDir, "yaml") else cacheDir
        val files = SystemFileSystem.list(targetDir)
            .filter { it.name.endsWith(".json") || it.name.endsWith(".yaml") }
            .filter { idFilter == null || it.name.contains(idFilter!!) }
        echo("Found ".plus(files.size).plus(" card files in ").plus(targetDir), err = true)

        var successCount = 0
        for (file in files) {
            try {
                echo("Processing: ".plus(file.name), err = true)
                val content = SystemFileSystem.source(file).buffered().use { it.readByteArray().decodeToString() }
                val jsonObject = if (file.name.endsWith(".yaml")) {
                    generator.yamlToJson(yaml.parseToYamlNode(content)).jsonObject
                } else {
                    json.parseToJsonElement(content).jsonObject
                }
                val cypher = generator.generateFromJson(jsonObject)
                sb.append(cypher).append("\n\n")
                successCount++
            } catch (e: Exception) {
                echo("Failed to process ".plus(file.name).plus(": ").plus(e.message), err = true)
            }
        }

        if (output != null) {
            val outPath = output!!
            // Ensure parent directories exist
            if (outPath.parent != null && !SystemFileSystem.exists(outPath.parent!!)) {
                SystemFileSystem.createDirectories(outPath.parent!!)
            }
            SystemFileSystem.sink(outPath).buffered().use { it.write(sb.toString().encodeToByteArray()) }
            echo("Successfully wrote ".plus(successCount).plus(" Cypher queries to ").plus(outPath), err = true)
        } else {
            echo(sb.toString())
            echo("Total processed: ".plus(successCount), err = true)
        }
    }
}
