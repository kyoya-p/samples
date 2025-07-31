import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

import kotlinx.serialization.Serializable

@Serializable
data class Echo(val message: String)

suspend fun requestEcho(server: String, from: String): Echo {
    val client = HttpClient(CIO) { install(ContentNegotiation) { json() } }
    return client.get("$server/echo?from=$from").body<Echo>()
}


fun Application.testServer() {
    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) { json() }
    routing {
        get("/echo") {
            val message = call.request.queryParameters["from"]?.let { "Hello, $it!" } ?: "Who are you?"
            call.respond(Echo(message = message))
        }
    }
}

suspend fun appMain() {
    val svr = embeddedServer(io.ktor.server.cio.CIO, 28080) {
        testServer()
    }.startSuspend(wait = false)

    val client = HttpClient(CIO) {
        defaultRequest { unixSocket("/tmp/test-unix-socket-client.sock") }
        install(ContentNegotiation) { json() }
    }
    client.get("$/echo?from=Shokkaa").body<Echo>()

    svr.stopSuspend()
}

