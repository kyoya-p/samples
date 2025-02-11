import com.charleskorn.kaml.Yaml
import gen_4.OpenApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath

expect val fileSystem: FileSystem

@ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
val yaml = Yaml.default

val json = Json { allowStructuredMapKeys = true }
// val topLevel = json.parse(TopLevel.serializer(), jsonString)

@OptIn(DelicateCoroutinesApi::class, ExperimentalSerializationApi::class)
suspend fun appMain() {
    fileSystem.listRecursively("apis".toPath()).forEach { path ->
        println("Path: $path")
        val schema = fileSystem.read(path) { readUtf8() }
        val openApi = yaml.decodeFromString<quicktype.openapi_3_0.TopLevelProperties>(schema)
        println(openApi)
    }
}
