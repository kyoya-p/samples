package basicauth

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.basic
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val user1 = UserPasswordCredential(name = "user1", password = "secret")

val testPort = 18080
fun server() = embeddedServer(Netty, port = testPort) {
    install(Authentication) {
        basic(name = "basicAuth1") {
            realm = "Ktor Server"
            validate { credentials ->
                when {
                    credentials == user1 -> UserIdPrincipal(credentials.name)
                    else -> null
                }
            }
        }
    }
    routing {
        authenticate("basicAuth1") {
            get("/") { call.respondText("Hello ${call.principal<UserIdPrincipal>()?.name}") }
        }
    }
}

fun basicAuthClient(user: String, password: String) = HttpClient(CIO) {
    install(Auth) {
        basic {
            credentials { BasicAuthCredentials(user, password) }
        }
    }
}
