import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.content.OutgoingContent.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import org.junit.Test
import java.io.File

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
    fun test2() = testApplication {
        application { module2() }
        val r = client.post("/s1") {
            body = object : ReadChannelContent() {
                override fun readFrom() = File("build.gradle.kts").readChannel()
            }
        }
        val ch = r.content
        generateFlow { ch.readUTF8Line() }.collect {
            println(it)
        }
    }
}

suspend fun <T> generateFlow(op: suspend () -> T?) = channelFlow<T> {
    while (true) {
        trySend(op() ?: break)
    }
}
