import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

val testPort = 18080
suspend fun server() = embeddedServer(Netty, port = testPort) {
    routing {
        get("/") { call.respondText("Hello") }
    }
}

class SimpleHTTPServer {

    @Test
    fun access1() = runBlocking {
        val svr = server().start(wait = false)
        val res: String = HttpClient(CIO).get { url("http://localhost:$testPort/") }.body()
        assert(res == "Hello")
        svr.stop()
    }
}
