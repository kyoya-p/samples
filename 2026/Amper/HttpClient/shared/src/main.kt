import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 引数なしの HttpClient() はクラスパスから利用可能なエンジンを自動選択します
    val client = HttpClient()
    try {
        println("Fetching https://example.com using ${client.engine}...")
        val response: HttpResponse = client.get("https://example.com")
        println("Status: ${response.status}")
        val body = response.bodyAsText()
        println("Body preview: ${body.take(100)}...")
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        client.close()
    }
}
