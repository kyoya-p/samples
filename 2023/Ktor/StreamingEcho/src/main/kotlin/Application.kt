import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(CIO, port = 8080) { module() }.start(wait = true)
}

fun Application.module() = routing {
    post("/") {
        val readChannel = call.receiveChannel()
        val text = readChannel.readRemaining().readText()
//        call.respondText(text)
        call.respondTextWriter(ContentType.Text.Plain, HttpStatusCode.OK) {
            delay(5.seconds)
            write(readChannel.readByte().toString())
            delay(5.seconds)
            write(readChannel.readByte().toString())
            delay(5.seconds)
            write(readChannel.readByte().toString())
        }
    }
    post("/streaming1") {
        val inputStream = call.receiveStream()
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach {
                call.respondText("[$it]")
            }
        }
    }
    post("/streaming2") {
        val receiveChannel = call.receiveChannel()
        try {
            while (!receiveChannel.isClosedForRead) {
                val packet = receiveChannel.readRemaining(1024)
                // Do something with the packet
            }
        } finally {
            receiveChannel.cancel()
        }
        call.respond("Stream received")
    }
}

