package jp.wjg.shokkaa.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath

@Serializable
class AppSettings(val apiKey: String = "", val fontSize: Int = 12)

val homeDir = System.getProperties().getProperty("user.home", ".")!!.toPath().resolve(".mcpbox")

//val fileSystem = SystemFileSystem
val json = Json { prettyPrint = true }
inline fun <reified T> String.decodeJson() = json.decodeFromString<T>(this)
inline fun <reified T> T.encodeJson() = json.encodeToString(this)

var appSettings
    get() = homeDir.toFile().readText().decodeJson<AppSettings>()
    set(a) = homeDir.toFile().writeText(a.encodeJson())
