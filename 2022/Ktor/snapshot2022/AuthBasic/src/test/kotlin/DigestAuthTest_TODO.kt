package digestauth

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class DigestAuthTest {

    @Test
    fun auth1(): Unit = runBlocking {
        val svr = digestAuthServer().start()
        val res = dynamicAuthClient(userId, password).get { url("http://localhost:$testPort") }
        assert(res.status == HttpStatusCode.OK)
        assert(res.body<String>() == "Hello $userId")
        svr.stop()
    }

    @Test
    fun auth2(): Unit = runBlocking {
        val svr = digestAuthServer().start()
        runCatching {
            val res = dynamicAuthClient(userId, "aaaa").get { url("http://localhost:$testPort") }.status
            assert(res == HttpStatusCode.Unauthorized)
        }
        svr.stop()
    }

    @Test
    fun auth3(): Unit = runBlocking {
        val svr = digestAuthServer().start()
        runCatching {
            val res = dynamicAuthClient(userId, password).get { url("http://localhost:$testPort") }.status
            assert(res == HttpStatusCode.Unauthorized)
        }
        svr.stop()
    }
}