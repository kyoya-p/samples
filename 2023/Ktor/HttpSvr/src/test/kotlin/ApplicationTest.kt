import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import org.junit.Test

class ApplicationTest {
    @Test
    fun test1() = testApplication {
        application { module() }
        val r = client.get("/").bodyAsText()
        assert(r == "Hello World!")
    }
}
