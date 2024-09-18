package org.example

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import kotlinx.serialization.Serializable

suspend fun main() {
    val apiKey = System.getenv("GOOGLE_API_KEY")
    gemini(apiKey, "fun fib():Int{} 完成させて").candidates.forEach {
        it.content.parts.forEach {
            println(it.text)
        }
    }
}


const val GEMINI_1_5_FLUSH =
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent"

suspend fun gemini(apiKey: String, directives: String): GeminiResponse {
    val client = HttpClient(CIO)
    val response = client.post("$GEMINI_1_5_FLUSH?key=$apiKey") {
        contentType(Json)
        setBody(GeminiRequest(listOf(Content(listOf(Part(directives))))))
    }
    return response.body<GeminiResponse>()
}

@Serializable
data class GeminiRequest(val contents: List<Content>)

@Serializable
data class Content(val parts: List<Part>)

@Serializable
data class Part(val text: String)


@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>,
    val usageMetadata: UsageMetadata
)

@Serializable
data class Candidate(
    val content: Content,
    val finishReason: String,
    val index: Int,
    val safetyRatings: List<SafetyRating>
)
//
//@Serializable
//data class Content(
//    val parts: List<Part>,
//    val role: String
//)
//
//@Serializable
//data class Part(
//    val text: String
//)

@Serializable
data class SafetyRating(
    val category: String,
    val probability: String
)

@Serializable
data class UsageMetadata(
    val promptTokenCount: Int,
    val candidatesTokenCount: Int,
    val totalTokenCount: Int
)