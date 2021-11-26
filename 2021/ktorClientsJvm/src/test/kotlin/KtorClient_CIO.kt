import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.ProxyBuilder.http
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.net.URL
import javax.net.ssl.TrustManager

class KtorClient_CIO {
    @Test
    fun t1(): Unit = runBlocking {
        val client = HttpClient(CIO) {
            engine {
                //proxy = ProxyBuilder.http("http://192.168.81.175:3080") //プロキシ設定
                https {
                    //trustManager = TrustManager()
                }
            }
        }
        val r = client.get<String>(URL("https://sec-auth03.nara.sharp.co.jp"))
        println(r)
    }

    @Test
    fun t2() {
    }
}