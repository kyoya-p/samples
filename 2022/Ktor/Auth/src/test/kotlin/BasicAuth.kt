import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.basic
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

val user1 = UserPasswordCredential(name = "user1", password = "secret")
//val user1 = BasicAuthCredentials(username = "user1", password = "secret")

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

class BasicAuth {
    @Test
    fun auth1(): Unit = runBlocking {
        val svr = server().start()
        val res: String = basicAuthClient(user1.name, user1.password).get { url("http://localhost:$testPort") }.body()
        assert(res == "Hello ${user1.name}")
        svr.stop()
    }
    @Test
    fun auth2(): Unit = runBlocking {
        val svr = server().start()
        val res = basicAuthClient(user1.name, "aaaa").get { url("http://localhost:$testPort") }.status
        assert(res == HttpStatusCode.Unauthorized)
        svr.stop()
    }
    @Test
    fun auth3(): Unit = runBlocking {
        val svr = server().start()
        val res = basicAuthClient("nnn", user1.password).get { url("http://localhost:$testPort") }.status
        assert(res == HttpStatusCode.Unauthorized)
        svr.stop()
    }
}