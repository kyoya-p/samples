package jp.wjg.shokkaa.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import kotlin.String

@Serializable
class AppSettings(
    val httpProxy: String? = null,
    val apiKey: String = System.getenv("GOOGLE_API_KEY") ?: "",
    val querys: List<String> = listOf(),
//    val mcpServices: Map<String, McpService> = builtinMcp,
    val mcpServices: List<McpService> = builtinMcp,
)

@Serializable
class McpService(
    val type: String = "stdio", // "port","stdio"
    val command: String = "node", // "node"
    val args: String = "node_modules\\npm\\bin\\npx-cli.js -y @playwright/mcp@latest --port 8931",
    val port: Int = 8931,
    val serviceUrl: String = "http://localhost:8931",
    val command1: String = "node.exe node_modules\\npm\\bin\\npx-cli.js -y @playwright/mcp@latest --port 8931"
)

val playwrightMcp = "playwright/mcp"
val builtinMcp = listOf(
    McpService(
        type = "stdin",
        command = "node",
        args = "node_modules\\npm\\bin\\npx-cli.js -y @playwright/mcp@latest",
        port = 8931,
        serviceUrl = "http://localhost:8931",
        command1 = "node.exe node_modules\\npm\\bin\\npx-cli.js -y @playwright/mcp@latest --port 8931",
    )
)

val appDir = System.getProperties().getProperty("user.home", ".")!!.toPath().resolve(".mcpbox")
val confFile = appDir.resolve("mcpbox.config")

val json = Json { prettyPrint = true }
inline fun <reified T> String.decodeJson() = json.decodeFromString<T>(this)
inline fun <reified T> T.encodeJson() = json.encodeToString(this)

// TODO
fun <T> T.copyBy(op: () -> T) {
    val v = this

}

var appSettings
    get() = runCatching { json.decodeFromString<AppSettings>(confFile.toFile().readText()) }.getOrElse { AppSettings() }
    set(a) = confFile.toFile().apply { parentFile.mkdirs() }.writeText(json.encodeToString(a))


