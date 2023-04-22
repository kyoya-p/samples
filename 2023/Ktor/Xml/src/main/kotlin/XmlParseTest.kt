import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.xml.Xml

@Serializable
data class Example(val foo: String)

suspend fun getXml(url: String): Example? {
    val client = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(json = Json { ignoreUnknownKeys = true })
        }
    }

    val xml = client.get<String>(url) {
        header("Accept", "application/xml")
    }

    return Xml.decodeFromString(xml)
}
