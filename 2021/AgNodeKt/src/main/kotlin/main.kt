import NodeJS.set
import io.ktor.client.*
import io.ktor.client.request.*

suspend fun main() {
    println("start client")
    process.env["NODE_TLS_REJECT_UNAUTHORIZED"] = "0" // TLS証明書チェックをバイパス

    val client = HttpClient()
    val httpRes = client.get<String>("https://google.com")
    println(httpRes)
}
