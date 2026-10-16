import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow

fun main() {
    embeddedServer(CIO, port = 8080, module = Application::module1).start(wait = true)
}

fun Application.module1() {
    install(ContentNegotiation) { json() }
    routing {
        route("/api") {
            get { call.respondText("No Params") }
            get("path/{p1}/{p2?}") { call.respondText { "${call.parameters["p1"]}/${call.parameters["p2"]}" } }
            get("query") { call.respondText("${call.request.queryParameters.toMap()}") }
            get("query/") { call.respondText("/${call.request.queryParameters.toMap()}") }
            post("post") { call.respondText(call.receiveText()) }
            post("stream") {
                //TODO

                flow<ByteArray> {val rcv=call.receiveChannel()
                    while (!rcv.isClosedForRead) {
                        val r=call.receiveChannel().readPacket(5)

                    }
                }
            }
        }
    }
}
