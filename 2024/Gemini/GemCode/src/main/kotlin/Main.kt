package org.example

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import okio.Path.Companion.toPath
import kotlinx.serialization.json.*

suspend fun main(args: Array<String>) {
    val apiKey = System.getenv("GOOGLE_API_KEY")
        ?: throw IllegalArgumentException("Not Found: Environment variable GOOGLE_API_KEY")
    val src = args[0].toPath().toFile().walk().filter { it.isFile && it.extension == "kt" }.joinToString {
        it.readText(Charsets.UTF_8)
    }
    val q1 = "証明して: フィボナッチ数列の隣接する二項は互いに素"
    val q2 = "コードレビューして: ${src} "
    val res = gemini(apiKey, q2)
//    val res = gemini_0(apiKey, q2)
    println(res)
}


const val GEMINI_1_5_FLUSH =
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent"
const val GEMINI_1_5_PRO =
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"

suspend fun gemini(apiKey: String, directives: String) = runCatching {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
//                ignoreUnknownKeys = true
            })
        }
    }
    val response = client.post("$GEMINI_1_5_PRO?key=$apiKey") {
        contentType(ContentType.Application.Json)
        setBody(GeminiRequest(listOf(Req_Content(listOf(Req_Part(directives))))))
    }
    response.body<GeminiResponse>()
}.getOrThrow()

suspend fun gemini_0(apiKey: String, directives: String) = runCatching {
    val client = HttpClient(CIO) { install(ContentNegotiation) { json() } }
    val response = client.post("$GEMINI_1_5_PRO?key=$apiKey") {
        contentType(ContentType.Application.Json)
        setBody(GeminiRequest(listOf(Req_Content(listOf(Req_Part(directives))))))
    }
    response.bodyAsText()
}.getOrThrow()

@Serializable
data class GeminiRequest(val contents: List<Req_Content>)

@Serializable
data class Req_Content(val parts: List<Req_Part>)

@Serializable
data class Req_Part(val text: String)

@Serializable
data class GeminiResponse(val candidates: List<Res_Candidate>, val usageMetadata: UsageMetadata)

@Serializable
data class Res_Candidate(
    val content: Res_Content,
    val finishReason: String,
    val index: Int,
    val safetyRatings: List<SafetyRating>,
    val citationMetadata: CitationMetadata? = null // nullable に設定
)

@Serializable
data class Res_Content(val parts: List<Res_Part>, val role: String)

@Serializable
data class Res_Part(val text: String)

@Serializable
data class SafetyRating(val category: String, val probability: String)

@Serializable
data class CitationMetadata(
    val citationSources: List<CitationSource>
)

@Serializable
data class CitationSource(
    val startIndex: Int,
    val endIndex: Int,
    val uri: String,
    val license: String? = null // nullable に設定
)

@Serializable
data class UsageMetadata(val promptTokenCount: Int, val candidatesTokenCount: Int, val totalTokenCount: Int)

