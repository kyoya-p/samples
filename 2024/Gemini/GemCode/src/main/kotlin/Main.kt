package org.example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

suspend fun main() {
    val apiKey = System.getenv("GOOGLE_API_KEY") ?: throw Exception("Not Found Env.Var. GOOGLE_API_KEY")
    gemini(apiKey, "fun fib():Int{} 完成させて")
//        .candidates.forEach {
//        it.content.parts.forEach {
//            println(it.text)
//        }
//    }

        .let { println(it) }
}


const val GEMINI_1_5_FLUSH =
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent"

suspend fun gemini(apiKey: String, directives: String): String {
    val client = HttpClient(CIO){
        install(ContentNegotiation){
            json()
        }
    }
    val response = client.post("$GEMINI_1_5_FLUSH?key=$apiKey") {
        contentType(ContentType.Application.Json)
        setBody(GeminiRequest(listOf(Req_Content(listOf(Req_Part(directives))))))
    }
    return response.bodyAsText()
}

@Serializable
data class GeminiRequest(val contents: List<Req_Content>)

@Serializable
data class Req_Content(val parts: List<Req_Part>)

@Serializable
data class Req_Part(val text: String)


@Serializable
data class GeminiResponse(
    val candidates: List<Res_Candidate>,
    val usageMetadata: UsageMetadata
)

@Serializable
data class Res_Candidate(
    val content: Res_Content,
    val finishReason: String,
    val index: Int,
    val safetyRatings: List<SafetyRating>
)

@Serializable
data class Res_Content(
    val parts: List<Res_Part>,
    val role: String
)

@Serializable
data class Res_Part(
    val text: String
)

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