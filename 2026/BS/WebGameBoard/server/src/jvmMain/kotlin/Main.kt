import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import java.io.File
import kotlinx.serialization.json.*

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }
        routing {
            get("/api/cards") {
                val sampleCards = listOf(
                    SearchCard("BS68-X01", "Phoenix Golem", "X", "8", "S", "Blue", listOf("Artificer", "Phoenix"), ""),
                    SearchCard("BS68-X02", "Sea King", "X", "7", "S", "Blue", listOf("Fighter"), "")
                )
                call.respond(sampleCards)
            }
            // Wasmアプリの配信設定
            staticFiles("/", File("compose-app/build/dist/wasmJs/productionExecutable")) {
                default("index.html")
            }
        }
    }.start(wait = true)
}
