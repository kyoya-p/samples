import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.server.testing.*
import org.junit.Test
import kotlin.test.assertEquals

class KtorTest {
    @Test
    fun test() = testApplication {
        application { serverApp() }
        val res = client.get("http://localhost/?user=tester").body<String>()
        println(res)
	assertEquals(res, "Hello, Tester.")
    }
}