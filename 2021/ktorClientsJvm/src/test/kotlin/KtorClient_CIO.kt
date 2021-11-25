import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.net.URL

class KtorClient_CIO {
    @Test
    fun t1(): Unit = runBlocking {
        val client = HttpClient(CIO)
        val r = client.get<String>(URL("https://google.com"))
        println(r)
    }

    @Test
    fun t2() {
    }
}