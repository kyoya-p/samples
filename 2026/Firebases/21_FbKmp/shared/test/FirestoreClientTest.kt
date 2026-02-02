import kotlin.test.Test
import kotlin.test.assertNotNull

class FirestoreClientTest {
    @Test
    fun testInitialization() {
        val client = RestFirestoreClient("api_key", "project_id")
        assertNotNull(client)
    }
}
