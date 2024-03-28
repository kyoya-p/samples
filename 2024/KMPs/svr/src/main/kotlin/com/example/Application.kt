import com.example.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.routing.*
import io.ktor.server.response.*

fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
        routing {
            get("/") {
                call.respondText("Hello World!")
            }
        }
    }.start(wait = true)
    println("http://127.0.0.1:8080/")
}

