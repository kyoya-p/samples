import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    val server = embeddedServer(Netty, port = 8080) {
        routing {
            get("/") {
                call.respondText("HELLO WORLD!")
            }
            get("/2") {
                call.respondText("Hello World!", ContentType.Text.Plain)
            }
        }
    }
    server.start(wait = true)
}
