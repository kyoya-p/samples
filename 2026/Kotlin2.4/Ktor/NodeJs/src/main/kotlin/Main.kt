import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

fun greeting(name: String) =
    "Hello, $name"

suspend fun main() {
    println(greeting("NodeJs"))

    // JSエンジン(ktor-client-js)が自動選択される
    val client = HttpClient()
    val response = client.get("https://www.google.com")

    if (response.status.value == 200) {
        println(response.bodyAsText())
    } else {
        println("Request failed with status code: ${response.status}")
    }
    client.close()
}
