import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException


fun main() {
    embeddedServer(Netty, port = 8080, module = appWebsocket).start(wait = true)
}

val appWebsocket: Application.() -> Unit = {
    install(WebSockets)
    install(CORS) {
        method(HttpMethod.Options)
        header(HttpHeaders.XForwardedProto)
        anyHost()
        allowCredentials = true
        allowNonSimpleContentTypes = true
    }
    routing {
        get("/") {
            call.respondText(
                "Hello getter! ${call.request.queryParameters.toMap()}"
            )
        }
        webSocket("/ws") {
            println("onConnect()")
            kotlin.runCatching {
                for (frame in incoming) {
                    println("onMessage")
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
            }.onFailure {
                when (it) {
                    is ClosedReceiveChannelException -> println("onClose ${closeReason.await()}")
                    else -> println("onError ${closeReason.await()}")
                }
            }.onSuccess {
                println("onClose ${closeReason.await()}");
            }
        }
    }
}