import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath

suspend fun main(args: Array<String>): Unit = with(FileSystem.SYSTEM) {
    val key = System.getenv("GOOGLE_API_KEY") ?: throw IllegalArgumentException("Not Found: GOOGLE_API_KEY")
    fun srcs() = listRecursively(".".toPath()).filter { it.name.endsWith(".kt") }.map { read(it) { readUtf8() } }
    val client = HttpClient(CIO) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
    client.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=$key") {
        contentType(ContentType.Application.Json)
        setBody(ReqGemini(listOf(ReqContent(listOf(ReqPart("コードレビューして: ${srcs().joinToString("\n")}"))))))
    }.body<ResGemini>().also(::println)
}

