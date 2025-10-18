import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.mock.*
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.client.request.get
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.let

class Tests1 : StringSpec({

    val mockEngine = MockEngine { request ->
        when (request.url.encodedPath) {
            "/hello" -> respond(request.url.parameters["from"]?.let { "Hello, $it!" } ?: "Who are you?")
            else -> respond("Unhandled ${request.url.encodedPath}", HttpStatusCode.BadRequest)
        }
    }

    "ktor OK1" {
        val client = HttpClient(mockEngine)
        val res = client.get("http://localhost/hello?from=Shokkaa")
        res.bodyAsText() shouldBe "Hello, Shokkaa!"
        res.status shouldBe HttpStatusCode.OK
    }
    "ktor OK2" {
        val client = HttpClient(mockEngine)
        val res = client.get("http://localhost/hello")
        res.bodyAsText() shouldBe "Who are you?"
        res.status shouldBe HttpStatusCode.OK
    }
    "ktor ERR1" {
        val client = HttpClient(mockEngine)
        val res = client.get("http://localhost/hellox")
        res.status shouldBe HttpStatusCode.BadRequest
    }
    "ktor TO1" {
        val client = HttpClient(CIO) { install(HttpTimeout) }
        shouldThrow<ConnectTimeoutException> {
            client.get("http://192.168.255.255/hello?from=Shokkaa").bodyAsText()
        }
    }
})

