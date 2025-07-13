package jp.wjg.shokkaa.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath

@Serializable
class AppSettings(
    val apiKey: String = System.getenv("GOOGLE_API_KEY") ?: "",
    val querys: List<String> = listOf(),
    val mcpServices: List<Mcp> = listOf(Mcp()),
)

@Serializable
class Mcp(
    val serviceUrl: String = "http://localhost:8931",
    val command: String = "node.exe node_modules\\npm\\bin\\npx-cli.js -y @playwright/mcp@latest --port 8931"
)

val appDir = System.getProperties().getProperty("user.home", ".")!!.toPath().resolve(".mcpbox")
val confFile = appDir.resolve("config")

val json = Json { prettyPrint = true }
inline fun <reified T> String.decodeJson() = json.decodeFromString<T>(this)
inline fun <reified T> T.encodeJson() = json.encodeToString(this)

// TODO
fun <T> T.copyBy(op: () -> T) {
    val v=this

}

var appSettings
    get() = runCatching { json.decodeFromString<AppSettings>(confFile.toFile().readText()) }.getOrElse { AppSettings() }
    set(a) = confFile.toFile().writeText(json.encodeToString(a))


