package org.example

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath


suspend fun main(args: Array<String>) {
    val apiKey = System.getenv("GOOGLE_API_KEY") ?: throw IllegalArgumentException("Not Found: GOOGLE_API_KEY")


    val src = args.getOrElse(0) { "." }.toPath().toFile().walk().filter { it.isFile && it.extension == "kt" }
        .joinToString("\n") { it.readText(Charsets.UTF_8) }
    val res = gemini<ResGemini>(apiKey, "コードレビューして: $src")
    println(res)
}

val GEMINI_1_0_PRO = "https://generativelanguage.googleapis.com/v1/models/gemini-pro:generateContent"
val GEMINI_1_5_FLUSH = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent"
val GEMINI_1_5_PRO = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"

fun httpClient() = HttpClient(CIO) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
suspend inline fun <reified T> gemini(apiKey: String, directives: String) =
    httpClient().post("$GEMINI_1_5_PRO?key=$apiKey") {
        contentType(ContentType.Application.Json)
        setBody(ReqGemini(listOf(ReqContent(listOf(ReqPart(directives))))))
    }.body<T>()

@Serializable
data class ReqGemini(val contents: List<ReqContent>)

@Serializable
data class ReqContent(val parts: List<ReqPart>)

@Serializable
data class ReqPart(val text: String)

@Serializable
data class ResGemini(
    val candidates: List<ResCandidate>,
//    val usageMetadata: UsageMetadata
)

@Serializable
data class ResCandidate(
    val content: ResContent,
//    val finishReason: String,
//    val index: Int,
//    val safetyRatings: List<SafetyRating>,
//    val citationMetadata: CitationMetadata? = null // nullable に設定
)

@Serializable
data class ResContent(val parts: List<ResPart>, val role: String)

@Serializable
data class ResPart(val text: String)

//@Serializable
//data class SafetyRating(val category: String, val probability: String)

//@Serializable
//data class CitationMetadata(
//    val citationSources: List<CitationSource>
//)

//@Serializable
//data class CitationSource(
//    val startIndex: Int,
//    val endIndex: Int,
//    val uri: String,
//    val license: String? = null // nullable に設定
//)

//@Serializable
//data class UsageMetadata(val promptTokenCount: Int, val candidatesTokenCount: Int, val totalTokenCount: Int)

