import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
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
}
