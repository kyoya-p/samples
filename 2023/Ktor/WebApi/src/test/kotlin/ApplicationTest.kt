import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import org.junit.Test

class ApplicationTest {
    @Test
    fun module1test1() = testApplication {
        application { module1() }
        assert(client.get("/").status.value == 404)
        assert(client.get("/api").bodyAsText() == "No Params")
    }
    @Test
    fun module1test2() = testApplication {
        application { module1() }
        assert(client.get("/api/path/p1/p2").bodyAsText() == "p1/p2")
    }
    @Test
    fun module1test3() = testApplication {
        application { module1() }
        assert(client.get("/api/query?a=1&b=x").bodyAsText() == "{a=[1], b=[x]}")
        assert(client.get("/api/query/?a=1&b=x").bodyAsText() == "/{a=[1], b=[x]}")
    }
    @Test
    fun module1test4() = testApplication {
        application { module1() }
        assert(client.post("/api/post"){setBody("a=b") }.bodyAsText() == "a=b")
    }
}

