import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.locations.*

@Location("/tn/{ip}/{port}/{login}/{pw}")
data class TelnetRequest(val ip: String, val port: Int)


fun main() {
    embeddedServer(CIO, port = 8000) {
        install(CORS)
        routing {
            get("/") {
                call.respondText("Hello, world!")
            }
        }
    }.start(wait = true)
}
