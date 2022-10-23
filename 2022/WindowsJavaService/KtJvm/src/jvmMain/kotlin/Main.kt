import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking

val testPort = 18080
suspend fun server() = embeddedServer(CIO, port = testPort) {
    install(ShutDownUrl.ApplicationCallPlugin) {
        shutDownUrl = "/off"
        exitCodeSupplier = { 0 }
    }
    routing { get("/") { call.respondText("Hello ${call.parameters["name"]}.") } }
}

fun main(): Unit = runBlocking {
    server().start(wait = true)
}
