package basicauth

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class BasicAuthTest {
    @Test
    fun auth1(): Unit = runBlocking {
        val svr = server().start()
        val res: String = basicAuthClient(user1.name, user1.password).get { url("http://localhost:$testPort") }.body()
        assert(res == "Hello ${user1.name}")
        svr.stop()
    }
    @Test
    fun auth2(): Unit = runBlocking {
        val svr = server().start()
        val res = basicAuthClient(user1.name, "aaaa").get { url("http://localhost:$testPort") }.status
        assert(res == HttpStatusCode.Unauthorized)
        svr.stop()
    }
    @Test
    fun auth3(): Unit = runBlocking {
        val svr = server().start()
        val res = basicAuthClient("nnn", user1.password).get { url("http://localhost:$testPort") }.status
        assert(res == HttpStatusCode.Unauthorized)
        svr.stop()
    }
}