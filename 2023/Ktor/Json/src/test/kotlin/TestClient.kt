import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import junit.framework.TestCase.assertEquals
import org.junit.Test

class TestClient {
    @Test
    fun test() = testApplication {
        application {
            serverApp()
        }
        val response = client.get("/customer")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(Customer(2, "two", "Shokkaa"), response.body<Customer>())
    }
}