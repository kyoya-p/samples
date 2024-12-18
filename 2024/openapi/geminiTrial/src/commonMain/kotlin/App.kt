import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.gemini.api.DefaultApi
import org.example.gemini.model.*

expect val GEMINI_API_KEY: String

suspend fun appGemini() {
    val client = HttpClient { install(ContentNegotiation) { json(Json) } }
    val api = DefaultApi("https://generativelanguage.googleapis.com/v1beta", client)
    val prompt = "GeminiとOpenAPIに関する今日のTipsを一つ教えて"
    val req = GenerateContentRequest(listOf(GenerateContentRequestContentsInner(listOf(Part(prompt)))))
    val res = api.generateContent(GEMINI_API_KEY, req).body()
    println(Json.encodeToString(res.candidates?.get(0)?.content?.parts?.get(0)?.text))
    client.close()
}