import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.ProxyBuilder.http
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.network.tls.*
import io.ktor.network.tls.certificates.*
import io.ktor.network.tls.extensions.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URL
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.text.toCharArray



class KtorClient_CIO {
    @Test
    fun t1(): Unit = runBlocking {
        val client = HttpClient(CIO) {
            engine {
                val pass = "changeit"
                //proxy = ProxyBuilder.http("http://192.168.81.175:3080") //プロキシ設定
                https {
                    val alias = "target"
                    val keystore = buildKeyStore {
                        certificate(alias) {
                            hash = HashAlgorithm.SHA256
                            sign = SignatureAlgorithm.ECDSA
                            keySizeInBits = 256
                            password = pass
                        }
                    }
                    keystore.load(File("cacerts").inputStream(), pass.toCharArray())

                    addKeyStore(keystore, "changeit".toCharArray() as CharArray?, "target")
                }
            }
        }
        val r = client.get<String>(URL("http://localhost"))
        println(r)
    }

    @Test
    fun t2() {
    }
}