import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import java.time.Duration
import java.util.*
import kotlin.collections.LinkedHashSet

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    val connections = Collections.synchronizedSet<Connection>(LinkedHashSet())

    routing {
        webSocket("/chat") {
            val c = Connection(this)
            connections += c
            c.session.send("Hello. ${connections.size} users are online")
            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val text = frame.readText()
                val message = "${c.name} $text"
                connections.forEach { it.session.send(message) }
            }
        }
    }
}