import google.generativeai.GoogleGenerativeAI
import kotlin.js.json
import kotlin.test.Test

class Chat {
    @Test
    fun chat() {
        val apiKey = getApiKey()
        val genai = GoogleGenerativeAI(apiKey)
        val model = genai.getGenerativeModel(json("model" to "gemini-1.5-flash"))
    }
}