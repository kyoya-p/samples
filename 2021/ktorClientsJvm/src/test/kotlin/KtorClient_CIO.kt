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
import java.net.URL
import java.security.KeyStore
import javax.net.ssl.TrustManager
import kotlin.text.toCharArray

class KtorClient_CIO {
    @Test
    fun t1(): Unit = runBlocking {
        val client = HttpClient(CIO) {
            engine {
                //proxy = ProxyBuilder.http("http://192.168.81.175:3080") //プロキシ設定
                https {
                    val alias = "target"
                    val keystore = buildKeyStore {

                        certificate(alias) {
                            hash = HashAlgorithm.SHA256
                            sign = SignatureAlgorithm.ECDSA
                            keySizeInBits = 256
                            password = "changeit"
                        }
                    }
                    keystore.load(File(""))

                    addKeyStore(keystore, "changeit".toCharArray() as CharArray?, "target")
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