import com.charleskorn.kaml.Yaml
import gen_3.OpenApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import okio.FileSystem
import okio.Path.Companion.toPath

expect val fileSystem: FileSystem

@ExperimentalSerializationApi
val yaml = Yaml.default

@OptIn(ExperimentalSerializationApi::class)
fun appMain() {
    fileSystem.listRecursively("apis".toPath()).forEach { path ->
        println("Path: $path")
        val schemaStr = fileSystem.read(path) { readUtf8() }
//        val yamlNode = yaml.parseToYamlNode(schema)
//        val openApi = yaml.decodeFromYamlNode<OpenApi>(yamlNode)
        val openApi = yaml.decodeFromString<OpenApi>(schemaStr)
        openApi.paths?.forEach { (k, v) ->
            v.post?.requestBody?.content?.forEach { (k, v) ->
                println("$k=${v.schema?.ref}")
            }
        }
    }
}

