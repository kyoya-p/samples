import google.generativeai.GEMINI_1_5_FLASH
import google.generativeai.GoogleGenerativeAI
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldHave
import kotlinx.coroutines.await
import kotlin.js.json

class MyFirstTestClass : FunSpec({
    test("generateContent()") {
        val apiKey = getApiKey()
        val genai = GoogleGenerativeAI(apiKey)
        val model = genai.getGenerativeModel(json("model" to GEMINI_1_5_FLASH))
        val result = model.generateContent("三角、四角、五角形、の次は? 簡潔に").await()
        result.response.text() should { it.contains("六角形") }
    }
})
