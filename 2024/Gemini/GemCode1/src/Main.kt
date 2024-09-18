import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

//TIP コードを<b>実行</b>するには、<shortcut actionId="Run"/> を押すか
// ガターの <icon src="AllIcons.Actions.Execute"/> アイコンをクリックします。
suspend fun main() {
    val apiKey = System.getenv("GOOGLE_API_KEY")
    gemini(apiKey,"")
}


const val GEMINI_1_5_FLUSH =
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent"

suspend fun gemini(apiKey: String, directives: String) {
    val client = HttpClient(CIO)
    val response = client.post("$GEMINI_1_5_FLUSH?key=$apiKey") {
        contentType(ContentType.Application.Json)
        setBody(Json.encodeToString(GeminiRequest(listOf(Content(listOf(Part(directives)))))))
    }
    return response
    println("Response: ${response.bodyAsText()}")
}

@Serializable
data class GeminiRequest(val contents: List<Content>)

@Serializable
data class Content(val parts: List<Part>)

@Serializable
data class Part(val text: String)