import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.unixConnector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import kotlinx.io.files.Path
import getOperatingSystem

import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

@Serializable
data class Echo(val message: String)

suspend fun appMain() {
    testService()
    if (!getOperatingSystem().startsWith("Windows")) {
        unixDomainSocket_TODO()
    }
}

fun Application.testService() {
    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) { json() }
    routing {
        get("/echo") {
            val message = call.request.queryParameters["from"]?.let { "Hello, $it!" } ?: "Who are you?"
            println("Resp: $message")
            call.respond(Echo(message = message))
        }
    }
}

suspend fun testService() {
    fun testServer(module: Application.() -> Unit) = embeddedServer(io.ktor.server.cio.CIO, port = 28080) { module() }

    val serverJob = testServer { testService() }.startSuspend()
    println("Start:")
    val client = HttpClient(CIO) { install(ContentNegotiation) { json() } }
    val res = client.get("http://localhost:28080/echo?from=Shokkaa").body<Echo>()
    println("Recv: ${res.message}")
    serverJob.stopSuspend()
}

suspend fun unixDomainSocket_TODO() {
    val socketPath = Path("./.test.sock")
    fun testServer_UnixConnector(module: Application.() -> Unit) = embeddedServer(io.ktor.server.cio.CIO, configure = {
        unixConnector(socketPath.toString())
    }, module = module)

    val serverJob = testServer_UnixConnector { testService() }.startSuspend()
    println("Start:")
    delay(100.milliseconds)
    val client = HttpClient(CIO) {
        defaultRequest { unixSocket("$socketPath") }
    }
    val res = client.get("$/echo?from=Shokkaa").bodyAsText()
    println("Recv: $res")
    serverJob.stopSuspend()
}
