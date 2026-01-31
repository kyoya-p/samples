import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.serialization.json.Json
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

import com.github.ajalt.clikt.core.Context

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple

class Neo : CliktCommand(name = "neo") {
    override fun help(context: Context) = "Neo4j DB Storage Tool (Direct Import)"
    
    private val json = Json { ignoreUnknownKeys = true }
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    init {
        context {
            helpOptionNames = setOf("-h", "--help")
            helpFormatter = { context -> MordantHelpFormatter(context, showDefaultValues = true) }
        }
    }

    val cardIds by argument(help = "Target Card IDs (exact match)").multiple()

    val cacheDir by option("-d", "--cache-dir", help = "Input directory containing YAML card files")
        .convert { Path(it) }.default(defaultCachePath)

    val url by option("--url", help = "Neo4j URL").default("neo4j://127.0.0.1:7687")
    val login by option("--login", help = "Neo4j Login (user:pass)").default("neo4j:00000000")

    override fun run() {
        if (!SystemFileSystem.exists(cacheDir)) {
            echo("Error: Cache directory not found: $cacheDir", err = true)
            return
        }

        val targetDir = if (SystemFileSystem.exists(Path(cacheDir, "yaml"))) Path(cacheDir, "yaml") else cacheDir
        
        val files = if (cardIds.isNotEmpty()) {
            cardIds.flatMap { id ->
                listOf(Path(targetDir, "$id.yaml"), Path(targetDir, "$id.json"))
                    .filter { SystemFileSystem.exists(it) }
            }
        } else {
            SystemFileSystem.list(targetDir)
                .filter { it.name.endsWith(".json") || it.name.endsWith(".yaml") }
        }
        
        if (files.isEmpty()) {
            echo("No card files found to import.", err = true)
            return
        }

        echo("Connecting to Neo4j at $url...", err = true)
        
        executeNeo4jImport(files)
    }

    private fun executeNeo4jImport(files: List<Path>) {
        // Implementation provided via expect/actual or conditional check for JVM
        // Since we are in shared code, we need to handle the JVM-only dependency.
        // For simplicity in this CLI, we'll try to use a common interface if we want cross-platform,
        // but here we'll use a platform-specific trigger.
        
        runImport(url, login, files, cacheDir)
    }
}

expect fun runImport(url: String, login: String, files: List<Path>, cacheDir: Path)
