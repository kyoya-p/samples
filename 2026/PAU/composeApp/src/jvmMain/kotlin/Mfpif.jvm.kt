import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.xml.xml
import java.net.InetAddress.getLocalHost
import java.net.NetworkInterface
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.X509TrustManager

actual fun client() = HttpClient(CIO) {
    install(ContentNegotiation) { xml() }
    engine {
        https {
            trustManager = trustAllCerts
        }
    }
}

// すべての証明書を信頼するTrustManager
val trustAllCerts = object : X509TrustManager {
    override fun getAcceptedIssuers(): Array<X509Certificate>? = null
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
}

// すべてのホスト名を許可するHostnameVerifier
val trustAllHostnameVerifier = HostnameVerifier { _, _ -> true }

actual fun getIpV4Host(): String = getLocalHost().hostAddress.trim()
actual fun getIpV4SubnetWidth(): Int {
    val ip = getIpV4Host()
    val localhost = getLocalHost()
    val networkInterface = NetworkInterface.getByInetAddress(localhost)
    if (networkInterface != null) {
        for (address in networkInterface.interfaceAddresses) {
            if (address.address == localhost) return address.networkPrefixLength.toInt()
        }
    }
    return 0
}