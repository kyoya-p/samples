import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(CIO, port = 8080) { module2() }.start(wait = true)
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
    post("/s1") {
        val channel = call.receiveChannel()
        while (!channel.isClosedForRead) {
            val bytes = channel.readUTF8Line()
            call.respondText("[$bytes]")
        }
    }
}

fun Application.module2() {
    routing {
        post("/") {
            call.respondTextWriter(contentType = ContentType.Text.Plain) {
                val channel = Channel<String>()
                launch(Dispatchers.IO) {
                    flow {
                        val recv = call.receiveChannel()
                        while (!recv.isClosedForRead) {
                            val r = recv.readUTF8Line() ?: break
                            println("Recv:$r")
                            emit(r)
                        }
                    }.collect {
                        channel.send("$it\n")
                    }
                    channel.close()
                }

                for (line in channel) {
                    write(line)
                    flush()
                }
            }
        }
        post("/s1") {
            call.respondTextWriter(contentType = ContentType.Text.Plain) {
                flow {
                    val recv = call.receiveChannel()
                    while (!recv.isClosedForRead) {
                        val r = recv.readUTF8Line() ?: break
                        println("Recv:$r")
                        emit(r)
                    }
                }.collect {
                    write("[$it]\n")
                    flush()
                }
            }
        }
    }
}