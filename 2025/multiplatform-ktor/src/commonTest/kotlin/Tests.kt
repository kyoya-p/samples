import io.kotest.core.spec.style.StringSpec
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.unixConnector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.time.Duration.Companion.milliseconds

fun Application.testServer() {
    install(ContentNegotiation) { json() }
    routing {
        get("/echo") {
            val message = call.request.queryParameters["from"]?.let { "Hello, $it!" } ?: "Who are you?"
            call.respond(Echo(message = message))
        }
    }
}

//TODO
val projDir = SystemFileSystem.resolve(Path("."))
//TODO
fun testServer_UnixConnector() = embeddedServer(CIO, configure = {
    unixConnector("")
}) {
}

class Tests : StringSpec({
    "HttpClient 1" {
        val serverJob = launch {
            embeddedServer(CIO, port = 28080, module = Application::testServer).startSuspend(wait = false)
            println("start server port:28080.")
        }
        delay(100.milliseconds)
        val result = requestEcho(server = "http://127.0.0.1:28080", from = "Shokkaa")
        println("Res:$result")
        serverJob.cancel()
    }

    //TODO
    "xxx" {
        println(projDir)
    }
})

