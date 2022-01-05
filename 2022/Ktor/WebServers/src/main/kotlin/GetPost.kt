import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/") {
                println("get/")
                call.respondText("Hello Get Request!", ContentType.Text.Plain)
            }
            post("/") {
                println("post/")
                call.respondText("Hello Post Request!", ContentType.Text.Plain)
            }
        }
    }.start(wait = true)
}