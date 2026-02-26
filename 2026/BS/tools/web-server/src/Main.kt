import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import java.io.File

fun main() {
    println("Starting Ktor Server on port 8080...")
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }
        
        routing {
            // Wasmアプリの成果物ディレクトリを静的ファイルとして公開
            val staticDir = File("build/tasks/_web-gameboard_linkWasmJs")
            println("Serving static files from: ${staticDir.absolutePath}")
            
            staticFiles("/", staticDir) {
                default("index.html")
            }
            
            get("/api/status") {
                call.respond(mapOf("status" to "ok"))
            }
        }
    }.start(wait = true)
}
