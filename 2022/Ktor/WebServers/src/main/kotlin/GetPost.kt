import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// implementation("io.ktor:ktor-server-core:1.6.7")
// implementation("io.ktor:ktor-server-netty:1.6.7")

@Suppress("JSON_FORMAT_REDUNDANT")
fun main() {
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/") {
                println("get/")
                val h = Json { prettyPrint = true }.encodeToString(call.request.headers.toMap())
                call.respondText("Hello Get Request! \n$h", ContentType.Text.Plain)            }
            post("/") {
                println("post/")
                val h = Json { prettyPrint = true }.encodeToString(call.request.headers.toMap())
                call.respondText("Hello Post Request! \n$h", ContentType.Text.Plain)            }
        }
    }.start(wait = true)
}