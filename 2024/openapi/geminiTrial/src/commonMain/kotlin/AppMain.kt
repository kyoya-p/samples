import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.gemini.api.DefaultApi
import org.example.gemini.model.*

expect val GEMINI_API_KEY: String

private val json = Json { ignoreUnknownKeys = true }

suspend fun appMain() {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }

    val api = DefaultApi("https://generativelanguage.googleapis.com/v1beta", client)
    val prompt = "GeminiとOpenAPIに関する今日のTipsを一つ教えて"
    val req = GenerateContentRequest(contents = listOf(Content(parts = listOf(Part(text = prompt)))))
    println(json.encodeToString(req))

    val res = api.modelsGemini15FlashgenerateContentPost(req).body()
    println(json.encodeToString(res.candidates?.get(0)?.content?.parts?.get(0)?.text))

    client.close()
}