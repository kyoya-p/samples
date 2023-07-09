import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.content.OutgoingContent.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import org.junit.Test
import java.io.File

import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.runBlocking

class ApplicationTest {

    fun now() = Clock.System.now()

    @Test
    fun test1() = testApplication {
        application { module() }
        val r = client.get("/") {
        }.bodyAsChannel()

        val r1 = r.readByte()
        println("Res(${now()})=$r1")
        val r2 = r.readByte()
        println("Res(${now()})=$r2")
        val r3 = r.readByte()
        println("Res(${now()})=$r3")
    }

    @OptIn(InternalAPI::class)
    @Test
    fun test2() = runBlocking{testApplication {
        application { module2() }
        val ch = ByteChannel()
        val r = client.post("/s2") {
            body = object : ReadChannelContent() {
                //                override fun readFrom() = File("build.gradle.kts").readChannel()
                override fun readFrom() = <Byte>{}
            }
        }
        var fib = 1L
        repeat(10) {
            ch.writeStringUtf8("$fib\n")
            ch.flush()
            fib = r.content.readUTF8Line()?.toLong() ?: return@repeat
            println(fib)
        }
//        val ch = r.content
    }}

    @Test
    fun t3()= runBlocking {
        val channel = produce {
            send("Hello")
            send("World")
            close()
        }

        channel.consumeEach { println(it) }
    }
}

