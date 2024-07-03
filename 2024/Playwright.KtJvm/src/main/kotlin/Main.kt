import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach

fun main(): Unit = embeddedServer(CIO, port = 8000) {
    install(WebSockets)
    routing {
        staticResources("/", ".")
        webSocket("/ws") {
            val uniqueId = generateNonce()
            println("Connection to $uniqueId established")

            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val msg = frame.readText()
                    outgoing.send(Frame.Text("[$msg]"))
                    if (frame.readText() == "close") {
                        close(CloseReason(CloseReason.Codes.NORMAL, "Closed by server"))
                    }
                }
            }
            println("Connection to $uniqueId closed")
        }
    }
    println("Start server.")
}.start(wait = true).let {}
