import org.neo4j.driver.AuthTokens
import org.neo4j.driver.GraphDatabase
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration

actual fun runImport(url: String, login: String, files: List<Path>, cacheDir: Path) {
    val (user, pass) = login.split(":").let { if (it.size == 2) it[0] to it[1] else "neo4j" to "00000000" }
    
    val generator = CypherGenerator()
    val json = Json { ignoreUnknownKeys = true }
    val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    try {
        GraphDatabase.driver(url, AuthTokens.basic(user, pass)).use { driver ->
            driver.session().use { session ->
                files.forEach { file ->
                    try {
                        print("Importing ${file.name}... ")
                        val content = SystemFileSystem.source(file).buffered().use { it.readByteArray().decodeToString() }
                        val jsonObject = if (file.name.endsWith(".yaml")) {
                            generator.yamlToJson(yaml.parseToYamlNode(content)).jsonObject
                        } else {
                            json.parseToJsonElement(content).jsonObject
                        }
                        val cypher = generator.generateFromJson(jsonObject)
                        session.run(cypher)
                        println("Done.")
                    } catch (e: Exception) {
                        println("Failed: ${e.message}")
                    }
                }
            }
        }
        println("Direct import to Neo4j completed.")
    } catch (e: Exception) {
        System.err.println("Database connection failed: ${e.message}")
    }
}
