import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.mapNotNull

// Websocket:
// https://jp.ktor.work/servers/features/websockets.html

fun main() {
    embeddedServer(CIO, port = 8000) {
        install(CORS)
        install(WebSockets)
        routing {
            webSocket("/tcp") {
                println("onConnect")
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            outgoing.send(Frame.Text("YOU SAID: $text"))
                            if (text.equals("bye", ignoreCase = true)) {
                                close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                            }
                        }
                    }
                }
                println("onClosed")
            }
        }
    }.start(wait = true)
}
