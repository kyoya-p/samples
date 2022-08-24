import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused", "JSON_FORMAT_REDUNDANT")
fun Application.module() {
    routing {
        get("/") {
            println("get/")
            val h = Json { prettyPrint = true }.encodeToString(call.request.headers.toMap())
            call.respondText("Hello Get Request! \n$h", ContentType.Text.Plain)
        }
    }
}
