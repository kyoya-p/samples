import google.generativeai.GEMINI_1_5_FLASH
import google.generativeai.GoogleGenerativeAI
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.js.*
import io.ktor.client.request.*
import kotlinx.coroutines.await
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import kotlin.js.json

class GeminiTestClass : FunSpec({
    test("generateContent") {
        val apiKey = getApiKey()
        val genai = GoogleGenerativeAI(apiKey)
        val model = genai.getGenerativeModel(json("model" to GEMINI_1_5_FLASH))
        val result = model.generateContent("三角、四角、五角形、の次は? 簡潔に").await()
        println(result.response.text())
        result.response.text() shouldContain "六角"
    }
    test("startChat") {
        val apiKey = getApiKey()
        val genai = GoogleGenerativeAI(apiKey)
        val model = genai.getGenerativeModel(json("model" to GEMINI_1_5_FLASH))
        val chat = model.startChat()
        chat.sendMessage("三角、四角、の次は? 簡潔に").await().response.text().also(::println) shouldContain "五角"
        chat.sendMessage("その次は? 簡潔に").await().response.text().also(::println) shouldContain "六角"
        chat.sendMessage("最後の答えの前は? 簡潔に").await().response.text().also(::println) shouldContain Regex("五角")
    }
    test("inlineFile") {
        val apiKey = getApiKey()
        val genai = GoogleGenerativeAI(apiKey)
        val model = genai.getGenerativeModel(json("model" to GEMINI_1_5_FLASH))

        //  { inlineData: { data: Buffer.from(fs.readFileSync(path)).toString("base64"), mimeType, } }
        data class InlineData(val data: String/*B64*/, val mimeType: String)

        val res = HttpClient(Js).get("https://pbs.twimg.com/media/GZVJoPRasAA4zla?format=jpg").body<ByteArray>()
        fileSystem.write("test.jpg".toPath(), true) { write(res) }

        val chat = model.startChat()
        chat.sendMessage()

//        val files = listOf(InlineData())
//        val chat = model.startChat()

    }
    test("count") {
        val projRoot = fileSystem.canonicalize("../../../..".toPath())
        val result = projRoot.resolve("build/count.txt").apply { fileSystem.delete(this) }
        fileSystem.listRecursively(projRoot.resolve("src"))
            .filter { it.name.endsWith(".kt") }.onEach { println(it.segments) }
            .map { it to fileSystem.read(it) { generateSequence { readUtf8Line() }.count() } }
            .forEach { p -> fileSystem.appendingSink(result).buffer().use { sink -> sink.writeUtf8("$p\n") } }
    }
})

