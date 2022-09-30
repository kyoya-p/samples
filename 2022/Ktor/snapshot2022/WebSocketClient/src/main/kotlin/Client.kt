import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    val client = HttpClient {
        install(WebSockets)
    }

    runBlocking {
        client.webSocket(port = 8080, path = "/chat") {
            val outgoingMessages = launch {
                while (true) {
                    val text = readln()
                    if (text == "exit") return@launch
                    send(text)
                }
            }
            val incommingMessages = launch {
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    println(frame.readText())
                }
            }

            outgoingMessages.join()
            incommingMessages.cancelAndJoin()
        }
    }
}