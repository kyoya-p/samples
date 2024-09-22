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
    val apiKey = System.getenv("GOOGLE_API_KEY") ?: throw IllegalArgumentException("Not Found: GOOGLE_API_KEY")
    fun src() = listRecursively(args.getOrElse(0) { "." }.toPath()).filter { it.name.endsWith(".kt") }
        .joinToString { read(it) { readUtf8() } }
    val client = HttpClient(CIO) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
    val res =
        client.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=$apiKey") {
            contentType(ContentType.Application.Json)
            setBody(ReqGemini(listOf(ReqContent(listOf(ReqPart("コードレビューして: ${src()}"))))))
        }.body<ResGemini>()
    println(res)
}

