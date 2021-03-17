import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*


fun main() {
    val server = embeddedServer(Netty, port = 8080) {
        install(WebSockets)
        routing {
            webSocket("/ws") {
                call.respondText("Hello World!", ContentType.Text.Plain)
                println("comeon")
            }
        }
    }
    server.start(wait = true)
}
