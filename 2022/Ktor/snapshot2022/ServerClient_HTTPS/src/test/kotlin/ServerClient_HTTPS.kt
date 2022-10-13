import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.network.tls.certificates.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

val testPort = 15443
fun server() = embeddedServer(Netty, applicationEngineEnvironment {
    val keyStoreFile = File("keystore.jks")
    val certAlias = "mycert"
    val storePass = "changeit"
    val certPass = "changeit"

    val keystore = generateCertificate(
        file = keyStoreFile,
        keyAlias = "mycert",
        keyPassword = "changeit",
        jksPassword = "changeit"
    )

    //connector { port = 15080 }
    sslConnector(
        keyStore = keystore,
        keyAlias = certAlias,
        keyStorePassword = { storePass.toCharArray() },
        privateKeyPassword = { certPass.toCharArray() }
    ) {
        port = testPort
        keyStorePath = keyStoreFile.absoluteFile
    }
    module {
        routing {
            get("/") { call.respondText("Hello") }
        }
    }
})

val clientCertUnchked = HttpClient(CIO) {
    engine {
        https {// 証明書チェック無効化(自己署名を許容)
            trustManager = object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOf()
                override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
                override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
            }
        }
    }
}


class ServerClient_HTTPS {
    @Test
    fun access1(): Unit = runBlocking {
        val svr = server().start(wait = false)
        val res: String = clientCertUnchked.get { url("https://localhost:$testPort/") }.body()
        assert(res == "Hello")
        svr.stop()
    }
}

