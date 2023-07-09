import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.content.OutgoingContent.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.datetime.Clock
import org.junit.Test
import java.io.File

import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

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
    fun test2() = runBlocking {
        testApplication {
            application { module2() }
            val sharedFlow = flow {
                for (i in 1..10) {
                    emit(i)
                    delay(1.seconds)
                }
            }.shareIn(this@runBlocking, started = SharingStarted.WhileSubscribed(), replay = 1)

            val ch = ByteChannel()
            val r = client.post("/s2") {
                body = object : ReadChannelContent() {
                    //                override fun readFrom() = File("build.gradle.kts").readChannel()
                    override fun readFrom() =
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
        }
    }

    @Test
    fun t3() = runBlocking {
        val flow = flow {
            for (i in 1..10) {
                emit(i)
            }
        }

        val channel = flow.asChannel()

        channel.consumeEach {
            println(it)
        }

    }
}

