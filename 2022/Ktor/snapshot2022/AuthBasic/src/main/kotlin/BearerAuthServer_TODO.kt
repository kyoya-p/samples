package bearerauthserver

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val testPort = 18080
val realm1 = "MyRealm"
val userId = "digest-user1"
val password = "secret"

fun bearerAuthServer() = embeddedServer(CIO, port = testPort) {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        get("/header_test") {
            call.respondText(call.request.headers["Authorization"]!!)
        }
    }
}

/*
*  https://ktor.io/docs/bearer-client.html
*
*/