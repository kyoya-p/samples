import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import org.junit.Test

class ApplicationTest {
    @Test
    fun test1() = testApplication {
        application { module() }
        val r = client.post("/"){
            setBody("12")
        }.bodyAsText()
        println("Res=$r")
//        assert(r == "Hello World!")
    }
}
