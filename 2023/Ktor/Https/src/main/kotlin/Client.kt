import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.security.cert.X509Certificate
import javax.net.ssl.*

suspend fun main()  {
    val client = HttpClient(CIO)
    val res = client.get("http://localhost:8080").bodyAsText()
    println("http:// $res")

    val clientHttps = HttpClient(CIO) {
        engine {
            https {
                // 証明書チェック無効
                trustManager = object : X509TrustManager {
                    override fun getAcceptedIssuers() = null
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                }
            }
        }
    }
    val resHttps = clientHttps.get("https://localhost:8443").bodyAsText()
    println("https:// $resHttps")
}
