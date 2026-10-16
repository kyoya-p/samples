import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.io.readString
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock.System.now

import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(CIO, port = 8080) { module2() }.start(wait = true)
}

fun Application.module() = routing {
    post("/") {
        val readChannel = call.receiveChannel()
        val text = readChannel.readBuffer().readString()
//        call.respondText(text)
        call.respondTextWriter(ContentType.Text.Plain, HttpStatusCode.OK) {
            // 受信済みテキストの先頭3バイトを、5秒間隔で1バイトずつ返す
            for (b in text.toByteArray().take(3)) {
                delay(5.seconds)
                write(b.toString())
                flush()
            }
        }
    }
    post("/s1") {
        val channel = call.receiveChannel()
        call.respondTextWriter(ContentType.Text.Plain) {
            while (!channel.isClosedForRead) {
                val line = channel.readLine() ?: break
                write("[$line]\n")
                flush()
            }
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
                            emit(recv.readLine() ?: break)
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
                        val r = recv.readLine() ?: break
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
                    val r = recv.readLine() ?: break
                    println("Recv: ${r}")
                    val n = r.toLongOrNull()
                    if (n == null) {
                        write("Error: not a number [$r]\n")
                        flush()
                        continue
                    }
                    seed += n
                    write("$seed\n")
                    flush()
                }
            }
        }
    }
}

// 呼び出し側スコープで生成。不要になったら Channel.cancel() で送信ループが終了する
fun CoroutineScope.sensorChannel(): Channel<Int> {
    val sensorDataChannel = Channel<Int>(Channel.UNLIMITED)
    launch {
        while (true) {
            sensorDataChannel.send(1)
            delay(1.seconds)
        }
    }
    return sensorDataChannel
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
            sensorCh.cancel()
        }
    }
}