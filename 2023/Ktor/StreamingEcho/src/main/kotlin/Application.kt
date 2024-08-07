import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock.System.now

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
                            emit(recv.readUTF8Line() ?: break)
                        }
                    }.collect { channel.send("$it\n") }
                    channel.close()
                }

                for (line in channel) {
                    write(line)
                    flush()
                }
            }
        }
        post("/s1") {
            var seed = 0L
            call.respondTextWriter(contentType = ContentType.Text.Plain) {
                flow {
                    val recv = call.receiveChannel()
                    while (!recv.isClosedForRead) {
                        val r = recv.readUTF8Line() ?: break
                        emit(r.toLong())
                        delay(0.5.seconds)
                    }
                }.collect {
                    write("${now()}[$it]\n")
                    flush()
                }
            }
        }
        post("/s2") {
            var seed = 0L
            println("/s2")
            call.respondTextWriter(contentType = ContentType.Text.Plain) {
                val recv = call.receiveChannel()
                while (!recv.isClosedForRead) {
                    println("Recv: ---")
                    val r = recv.readUTF8Line() ?: break
                    println("Recv: ${r}")
                    seed += r.toLong()
                    write("$seed\n")
                    flush()
                }
            }
        }
    }
}

suspend fun sensorChannel(): Channel<Int> = coroutineScope {
    val sensorDataChannel = Channel<Int>(Channel.UNLIMITED)
    launch {
        while (true) {
            sensorDataChannel.send(1)
            delay(1.seconds)
        }
    }
    sensorDataChannel
}


fun Application.module3() = routing {
    post("/") {
        val sensorCh = sensorChannel()
        println("/")
        call.respondTextWriter(contentType = ContentType.Text.Plain) {
            repeat(15) {
                val s = sensorCh.receive()
                write("Sensor:$s\n")
                flush()
                println("Sensor:$s\n")
            }
            close()
        }
    }
}