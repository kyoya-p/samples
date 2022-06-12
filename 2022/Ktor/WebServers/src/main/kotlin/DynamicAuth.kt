import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// implementation("io.ktor:ktor-server-core:1.6.7")
// implementation("io.ktor:ktor-server-netty:1.6.7")
// implementation("io.ktor:ktor-auth:1.6.7") // Basic認証,Digest認証等

@Suppress("JSON_FORMAT_REDUNDANT")
fun main() {
    embeddedServer(Netty, port = 8080) {
        install(Authentication) {
            val myRealm = "MyRealm"
            val usersInMyRealmToHA1: Map<String, ByteArray> = mapOf(
                // pass="test", HA1=MD5("test:MyRealm:pass")="fb12475e62dedc5c2744d98eb73b8877"
                "test" to hex("fb12475e62dedc5c2744d98eb73b8877")
            )
            digest("auth-digest") {
                realm = myRealm
                digestProvider { userName, realm ->
                    usersInMyRealmToHA1[userName]
                }
            }
        }

        routing {
            authenticate("auth-digest") {
                get("/") {
                    println("get/")
                    val h = Json { prettyPrint = true }.encodeToString(call.request.headers.toMap())
                    call.respondText("Hello Get Request! \n$h", ContentType.Text.Plain)
                }
            }
        }
    }.start(wait = true)
}