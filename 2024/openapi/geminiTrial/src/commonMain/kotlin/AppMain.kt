import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.example.gemini.api.DefaultApi
import org.example.gemini.model.ModelsGemini15FlashGenerateContentPostRequest
import org.example.gemini.model.ModelsGemini15FlashGenerateContentPostRequestContentsInner
import org.example.gemini.model.ModelsGemini15FlashGenerateContentPostRequestContentsInnerPartsInner

expect val GEMINI_API_KEY: String

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

    val url = "https://generativelanguage.googleapis.com/v1beta"
    val api = DefaultApi(url, client)

    val prompt = "GeminiとOpenAPIに関する今日のTipsを一つ教えて"

    val requestBody = ModelsGemini15FlashGenerateContentPostRequest(
        contents = listOf(
            ModelsGemini15FlashGenerateContentPostRequestContentsInner(
                parts = listOf(
                    ModelsGemini15FlashGenerateContentPostRequestContentsInnerPartsInner(text = prompt)
                )
            )
        )
    )
    runCatching {
        val response = api.modelsGemini15FlashgenerateContentPost(GEMINI_API_KEY, requestBody)
        println(response.body())
    }.onFailure { e -> println("Error: ${e.message}") }

    client.close()
}