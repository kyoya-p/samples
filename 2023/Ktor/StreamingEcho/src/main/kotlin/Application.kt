import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(CIO, port = 8080) { module() }.start(wait = true)
}

fun Application.module() = routing {
    post("/") {
        val channel = call.receiveChannel()
        while (!channel.isClosedForRead) {
            val rs = channel.readByte() ?: break
            call.respondText("[$rs]")
        }
    }
}

