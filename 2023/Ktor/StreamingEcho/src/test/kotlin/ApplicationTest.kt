import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.content.OutgoingContent.*
import io.ktor.server.testing.*
import io.ktor.test.dispatcher.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.datetime.Clock
import org.junit.Test
import java.io.File

import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    fun t3() = testApplication {
        runBlocking(Dispatchers.Default) {
            application { module3() }
            val ch = ByteChannel()
            val r = client.post("/") {
                body = object : ReadChannelContent() {
                    override fun readFrom(): ByteReadChannel = ch
                }
            }
            launch {
                while (!r.content.isClosedForRead) {
                    println(r.content.readUTF8Line())
                }
            }
            println("sending..")
            launch {
                ch.writeStringUtf8("123\n")
                ch.flush()
                ch.close()
            }
        }
    }
}

