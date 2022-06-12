import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*

fun main() {
    val server = embeddedServer(Netty, port = 8080) {
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
