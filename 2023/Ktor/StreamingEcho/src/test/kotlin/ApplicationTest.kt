import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.coroutines.flow.channelFlow
import kotlinx.datetime.Clock
import org.junit.Test

class ApplicationTest {

    fun now() = Clock.System.now()

    @Test
    fun test1() = testApplication {
        application { module() }
        val r = client.post("/") {
        }.bodyAsChannel()

        val r1 = r.readByte()
        println("Res(${now()})=$r1")
        val r2 = r.readByte()
        println("Res(${now()})=$r2")
        val r3 = r.readByte()
        println("Res(${now()})=$r3")
    }
}