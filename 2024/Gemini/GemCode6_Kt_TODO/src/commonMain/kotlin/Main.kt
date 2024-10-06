import google.generativeai.GoogleGenerativeAI
import google.generativeai.TextPart
import kotlinx.coroutines.await
import kotlin.js.json

val GEMINI_1_0_PRO = "gemini-1.0-pro"
val GEMINI_1_5_FLASH = "gemini-1.5-flash"
val GEMINI_1_5_PRO = "gemini-1.5-pro"

expect fun getApiKey(): String

suspend fun main() {
    val apiKey = getApiKey()
    val ai = GoogleGenerativeAI(apiKey)
    val model = ai.getGenerativeModel(json("model" to GEMINI_1_5_FLASH))
    val result = model.generateContent("三角、四角、五角形、の次は? 簡潔に").await()
    println(result.response.text())
}

