import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*

fun main() {
    embeddedServer(CIO, port = 8082) {
        serverApp()
    }.start(wait = true)
}

fun Application.serverApp() {
    routing {
        get("/") {
            call.respondText("Hello, Tester.")
        }
    }
}
