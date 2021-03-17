import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*


fun main() {
    val server = embeddedServer(Netty, port = 8080) {
        install(CORS) {
            method(HttpMethod.Options)
            header(HttpHeaders.XForwardedProto)
            anyHost()
            allowCredentials = true
            allowNonSimpleContentTypes = true
        }
        routing {
            get("/") {
                call.respondText(
                    "HELLO ${call.request.queryParameters.toMap()}"
                )
            }
        }
    }
    server.start(wait = true)
}
