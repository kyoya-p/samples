@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package google.generativeai

import kotlin.js.Json
import kotlin.js.Promise

val GEMINI_1_0_PRO = "gemini-1.0-pro"
val GEMINI_1_5_FLASH = "gemini-1.5-flash"
val GEMINI_1_5_PRO = "gemini-1.5-pro"

expect class GoogleGenerativeAI(apiKey: String) {
    fun getGenerativeModel(params: Json): GenerativeModel
}

expect class GenerativeModel {
    fun generateContent(request: String): Promise<GenerateContentResult>
}

expect class CountTokensResponse {
    val totalTokens: Int
}

expect sealed class Part
expect class TextPart : Part {
    val text: String
}

//class InlineDataPart : Part
//class FunctionCallPart : Part
//class FunctionResponsePart : Part
//class FileDataPart : Part
//class ExecutableCodePart : Part
//class CodeExecutionResultPart : Part

expect class GenerateContentResult {
    val response: EnhancedGenerateContentResponse
}

expect class EnhancedGenerateContentResponse : GenerateContentResponse {
    fun text(): String
}

expect open class GenerateContentResponse {
    val candidates: Array<GenerateContentCandidate>?

    //        val  promptFeedback: PromptFeedback?
    val usageMetadata: UsageMetadata?
}

expect class GenerateContentCandidate {
    val index: Int
    val content: Content
//            val finishReason: FinishReason?
//            val finishMessage: String?
//            val safetyRatings: Array<SafetyRating>?
//            val citationMetadata: CitationMetadata?
}

expect class UsageMetadata {
    val promptTokenCount: Int
    val candidatesTokenCount: Int
    val totalTokenCount: Int
    val cachedContentTokenCount: Int?
}

expect class Content {
    val role: String
    val parts: Array<Part>
}
