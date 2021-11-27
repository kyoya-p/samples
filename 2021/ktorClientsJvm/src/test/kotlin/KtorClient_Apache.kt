import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.apache.*
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
import java.security.SecureRandom

import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager


class KtorClient_Apache {
    class TrustAllX509TrustManager : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOfNulls(0)
        override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
        override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
    }

    @Test
    fun t1(): Unit = runBlocking {
        val client = HttpClient(Apache) {
            engine {
                sslContext = SSLContext.getInstance("TLS")
                    .apply {
                        init(null, arrayOf(TrustAllX509TrustManager()), SecureRandom())
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