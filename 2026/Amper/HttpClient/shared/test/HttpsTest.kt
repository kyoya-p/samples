import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpsTest {
    @Test
    fun testGetExample() = runTest {
        val client = HttpClient()
        try {
            val response: HttpResponse = client.get("https://example.com")
            assertEquals(200, response.status.value)
            assertTrue(response.bodyAsText().contains("Example Domain"))
        } finally {
            client.close()
        }
    }
}
