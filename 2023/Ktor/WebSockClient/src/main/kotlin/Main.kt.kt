import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

suspend fun main() {
    val client = HttpClient(CIO) {
        install(WebSockets)
    }

    client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/ws") {
        val inc = launch {
            runCatching {
                for (message in incoming) {
                    message as? Frame.Text ?: continue
                    println(message.readText())
                }
            }.onFailure { e -> println("Error while receiving: " + e.localizedMessage) }
        }
        launch {
            runCatching {
                while (true) {
                    val message = readLine() ?: ""
                    if (message.equals("exit", true)) return@launch
                    send(message)
                }
            }.onFailure { e ->
                println("Error while sending: " + e.localizedMessage)
            }
        }.join()
        inc.cancelAndJoin()
    }
}
