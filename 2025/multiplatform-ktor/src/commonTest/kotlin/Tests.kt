import io.kotest.core.spec.style.StringSpec
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.io.files.Path
import kotlin.time.Duration.Companion.milliseconds

fun Application.testService() {
    install(ContentNegotiation) { json() }
    routing {
        get("/echo") {
            val message = call.request.queryParameters["from"]?.let { "Hello, $it!" } ?: "Who are you?"
            println("Resp: $message")
            call.respond(Echo(message = message))
        }
    }
}

class Tests : StringSpec({
//    "HttpClient 1" {
//        val serverJob = launch {
//            embeddedServer(CIO, port = 28080, module = Application::testServer).startSuspend(wait = false)
//            println("start server port:28080.")
//        }
//        delay(100.milliseconds)
//        val result = requestEcho(server = "http://127.0.0.1:28080", from = "Shokkaa")
//        println("Res:$result")
//        serverJob.cancel()
//    }

    "uniDomainSocket" {
//        val socketPath = SystemFileSystem.run { Path("./.test.sock").also { sink(it).buffered().run { writeString("") } } }
        val socketPath = Path("./.test.sock")
        fun testServer_UnixConnector(module: Application.() -> Unit) = embeddedServer(CIO, configure = {
            unixConnector(socketPath.toString())
        }, module = module)

        val serverJob = testServer_UnixConnector { testService() }.startSuspend()
        println("Start:")
        delay(100.milliseconds)
        val client = HttpClient(io.ktor.client.engine.cio.CIO) {
            defaultRequest { unixSocket("$socketPath") }
        }
        val res = client.get("$/echo?from=Shokkaa").bodyAsText()
        println("Recv: $res")
        serverJob.stopSuspend()
    }
})

