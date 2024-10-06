import google.generativeai.GEMINI_1_5_FLASH
import google.generativeai.GoogleGenerativeAI
import kotlinx.coroutines.await
import kotlin.js.json

suspend fun main() {
    val apiKey = getApiKey()
    val genai = GoogleGenerativeAI(apiKey)
    val model = genai.getGenerativeModel(json("model" to GEMINI_1_5_FLASH))
    val result = model.generateContent("三角、四角、五角形、の次は? 簡潔に").await()
    println(result.response.text())
}

