import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*

fun main() {
    val server = embeddedServer(Netty, host = "192.168.11.5", port = 8080) {
        routing {
            get("/") {
                call.respondText(
                    "HELLO ${call.request.queryParameters.toMap()}"
                )
            }
        }
    }
    server.start(wait = true)
}
