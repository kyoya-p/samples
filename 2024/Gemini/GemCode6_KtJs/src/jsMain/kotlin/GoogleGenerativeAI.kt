// 名前空間(複数クラス等)をexportするNPMモジュールを参照する場合、packageに対して@JsModuleを付与する

@file:Suppress("unused", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@file:JsModule("@google/generative-ai") // NPMモジュール名
@file:JsNonModule // モジュールとしてではなく、グローバルにアクセス可能にする


package google.generativeai

import kotlin.js.Json
import kotlin.js.Promise

actual external class GoogleGenerativeAI actual constructor(apiKey: String) {
    actual fun getGenerativeModel(params: Json): GenerativeModel
}

actual external class GenerativeModel {
    //    generateContent(request: GenerateContentRequest | string | Array<string | Part>, requestOptions?: SingleRequestOptions)
    actual fun generateContent(request: String): Promise<GenerateContentResult>

//    actual suspend  fun generateContent(request: String): GenerateContentResult
//    actual   fun generateContent(request: Array<String>): Promise<GenerateContentResult>
//    actual   fun generateContent(request: Array<Part>): Promise<GenerateContentResult>
//     fun generateContent(request: GenerateContentRequest): Promise<GenerateContentResult>

    actual fun startChat(): ChatSession
    actual fun startChat(startChatParams: StartChatParams?): ChatSession

    fun countTokens(request: String): Promise<CountTokensResponse>
}

actual external class StartChatParams {
    actual val history: Array<Content>?
//    actual val tools: Array<Tool>?
//    actual val toolConfig: ToolConfig?
//    actual val systemInstruction: String? // | Part | Content
//    actual val cachedContent: String?
}

actual external class ChatSession {
    //     sendMessage(request: string | Array<string | Part>, requestOptions?: SingleRequestOptions): Promise<GenerateContentResult>;
    actual fun sendMessage(request: String): Promise<GenerateContentResult>
    actual fun sendMessage(request: String, files: List<InlineImage>): Promise<GenerateContentResult>
}

actual external class CountTokensResponse {
    actual val totalTokens: Int
}

actual external sealed class Part
actual external class TextPart : Part {
    actual val text: String
}

//class InlineDataPart : Part
//class FunctionCallPart : Part
//class FunctionResponsePart : Part
//class FileDataPart : Part
//class ExecutableCodePart : Part
//class CodeExecutionResultPart : Part

actual external class GenerateContentResult {
    actual val response: EnhancedGenerateContentResponse
}

actual external class EnhancedGenerateContentResponse : GenerateContentResponse {
    actual fun text(): String
}

actual external open class GenerateContentResponse {
    actual val candidates: Array<GenerateContentCandidate>?

    //        val  promptFeedback: PromptFeedback?
    actual val usageMetadata: UsageMetadata?
}

actual external class GenerateContentCandidate {
    actual val index: Int
    actual val content: Content
//            val finishReason: FinishReason?
//            val finishMessage: String?
//            val safetyRatings: Array<SafetyRating>?
//            val citationMetadata: CitationMetadata?
}

actual external class UsageMetadata {
    actual val promptTokenCount: Int
    actual val candidatesTokenCount: Int
    actual val totalTokenCount: Int
    actual val cachedContentTokenCount: Int?
}

actual external class Content {
    actual val role: String
    actual val parts: Array<Part>
}
