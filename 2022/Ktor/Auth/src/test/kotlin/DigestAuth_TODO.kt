package auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.security.MessageDigest

val testPort = 18080
val realm1 = "MyRealm"
val userId = "digest-user1"
val password = "secret"

fun digestAuthServer() = embeddedServer(Netty, port = testPort) {
    install(Authentication) {
        val md5 = MessageDigest.getInstance("MD5")
        digest("digest1") {
            realm = realm1
            digestProvider { userName, realm ->
                when (userName) {
                    "missing" -> null
                    else -> {
                        md5.reset()
                        md5.update("$userName:$realm:$password".toByteArray())
                        md5.digest()
                    }
                }
            }
        }
    }

    routing {
        authenticate("digest1") {
            get("/") {
                val user = call.principal<UserIdPrincipal>()
                call.respondText("Hello ${user?.name}")
            }
        }
    }
}

fun dynamicAuthClient(user: String, password: String) = HttpClient(CIO) {
    install(Auth) {
        digest {
            credentials { DigestAuthCredentials(user, password) }
        }
    }
}


class DigestAuth {

    @Test
    fun auth1(): Unit = runBlocking {
        val svr = digestAuthServer().start()
        val res = dynamicAuthClient(userId, password).get { url("http://localhost:$testPort") }
        assert(res.status == HttpStatusCode.OK)
        assert(res.body<String>() == "Hello $userId")
        svr.stop()
    }

    @Test
    fun auth2(): Unit = runBlocking {
        val svr = digestAuthServer().start()
        runCatching {
            val res = dynamicAuthClient(userId, "aaaa").get { url("http://localhost:$testPort") }.status
            assert(res == HttpStatusCode.Unauthorized)
        }
        svr.stop()
    }

    @Test
    fun auth3(): Unit = runBlocking {
        val svr = digestAuthServer().start()
        runCatching {
            val res = dynamicAuthClient(userId, password).get { url("http://localhost:$testPort") }.status
            assert(res == HttpStatusCode.Unauthorized)
        }
        svr.stop()
    }
}