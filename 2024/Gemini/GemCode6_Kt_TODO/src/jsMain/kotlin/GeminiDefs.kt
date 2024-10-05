import kotlinx.serialization.Serializable
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun httpClient() = HttpClient(Js) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
suspend inline fun <reified T> gemini(apiKey: String, directives: String) =
    httpClient().post("https://generativelanguage.googleapis.com/$GEMINI_1_5_FLASH:generateContent?key=$apiKey") {
        contentType(ContentType.Application.Json)
        setBody(ReqGemini(listOf(ReqContent(listOf(ReqPart(directives))))))
    }.body<T>()

val GEMINI_1_0_PRO = "v1/models/gemini-pro"
val GEMINI_1_5_FLASH = "v1beta/models/gemini-1.5-flash"
val GEMINI_1_5_PRO = "v1beta/models/gemini-pro"

@Serializable
data class ReqGemini(val contents: List<ReqContent>)

@Serializable
data class ReqContent(val parts: List<ReqPart>)

@Serializable
data class ReqPart(val text: String)

@Serializable
data class ResGemini(
    val candidates: List<ResCandidate>, val usageMetadata: ResUsageMetadata
)

@Serializable
data class ResCandidate(
    val content: ResContent,
    val finishReason: String,
    val index: Int,
    val safetyRatings: List<ResSafetyRating>,
    val citationMetadata: ResCitationMetadata? = null
)

@Serializable
data class ResContent(val parts: List<ResPart>, val role: String)

@Serializable
data class ResPart(val text: String)

@Serializable
data class ResSafetyRating(val category: String, val probability: String)

@Serializable
data class ResCitationMetadata(val citationSources: List<ResCitationSource>)

@Serializable
data class ResCitationSource(
    val startIndex: Int, val endIndex: Int, val uri: String, val license: String? = null
)

@Serializable
data class ResUsageMetadata(val promptTokenCount: Int, val candidatesTokenCount: Int, val totalTokenCount: Int)

