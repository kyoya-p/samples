import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(Authentication) {
            basic(name = "basicAuth1") {
                realm = "Ktor Server"
                validate { credentials ->
                    if (credentials.name == credentials.password) { // user=nameのとき認証パス
                        UserIdPrincipal(credentials.name)
                    } else {
                        null
                    }
                }
            }
        }
        routing {
            authenticate("basicAuth1") {
                get("/") {
                    println("get/")
                    call.respondText("Hello Get Request!", ContentType.Text.Plain)
                }
            }
        }
    }.start(wait = true)
}