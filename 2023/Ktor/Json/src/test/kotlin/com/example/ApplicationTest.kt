import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.server.testing.*
import org.junit.Test

class ApplicationTest {
    @Test
    fun test1(): Unit = testApplication {
        application { sampleModule() }
        val client = sampleClient(client.engine)
        val r = clientSample1(client)
        assert(r == testData)
    }

    @Test
    fun test2(): Unit = testApplication {
        application { sampleModule() }
        val client = sampleClient(client.engine)
        val r = clientSample2(client, 2)
        assert(r == testData.filter { it.id == 2 })
    }

    @Test
    fun test3(): Unit = testApplication {
        application { sampleModule() }
        val client = sampleClient(client.engine)
        val r = clientSample3(client, 2)
        assert(r == testData.filter { it.id == 2 })
    }

    @Test
    fun test4(): Unit = testApplication {
        application { sampleModule() }
        val client = sampleClient(client.engine)
        val r = clientSample4(client, Customer(9, "Test", "Test"))
        assert(r == Created)
        val r2 = clientSample2(client, 9).getOrNull(0)
        assert(r2?.firstName == "Test")
    }
}
