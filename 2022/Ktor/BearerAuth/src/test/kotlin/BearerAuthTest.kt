import bearerauthserver.bearerAuthServer
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class BearerAuthTest {
    @Test
    fun test1() = runBlocking {
        val svr = bearerAuthServer().start()
//        val res: String = basicAuthClient(user1.name, user1.password).get { url("http://localhost:$testPort") }.body()
//        assert(res == "Hello ${user1.name}")
        svr.stop()

    }
}