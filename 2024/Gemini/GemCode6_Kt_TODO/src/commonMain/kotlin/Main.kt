import google.generativeai.GoogleGenerativeAI
import kotlinx.coroutines.await
import kotlin.js.json

external interface Process {
    interface Env {
        val GOOGLE_API_KEY: String?
    }

    val args: Array<String>
    val env: Env
}

external val process: Process
val GEMINI_1_0_PRO = "gemini-1.0-pro"
val GEMINI_1_5_FLASH = "gemini-1.5-flash"
val GEMINI_1_5_PRO = "gemini-1.5-pro"

suspend fun main() {
    val apiKey = js("process.env.GOOGLE_API_KEY") as? String ?: throw IllegalArgumentException("No GOOGLE_API_KEY.")
    val ai = GoogleGenerativeAI(apiKey)
    val model = ai.getGenerativeModel(json("model" to GEMINI_1_5_FLASH))
    val result = model.generateContent("三角、四角、五角形、の次は? 簡潔に").await()
    result.response.candidates?.forEach {
        it.content.parts.forEach { part ->
            val r = when {
                js("'text' in part") -> js("part.text") as String
                else -> "No Text."
            }
            println(r)
        }
    }
    println("totalTokenCount:         ${result.response.usageMetadata?.totalTokenCount}")
    println("promptTokenCount:        ${result.response.usageMetadata?.promptTokenCount}")
    println("candidatesTokenCount:    ${result.response.usageMetadata?.candidatesTokenCount}")
    println("cachedContentTokenCount: ${result.response.usageMetadata?.cachedContentTokenCount}")

}

