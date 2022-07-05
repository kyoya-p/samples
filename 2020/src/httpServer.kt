import com.github.kittinunf.fuel.httpGet
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

fun main() {
    useInsecureSSL()

    val server = HttpServer.create(InetSocketAddress(8000), 0)
    server.createContext("/", MyHandler { t ->
        val url = t.requestURI!!
        val reqHeader = t.responseHeaders!!
        val reqBody = t.requestBody.readAllBytes()!!

        val result = "https://xxxxxxx".httpGet().response()
        val response = result.second

        response.headers.forEach { (k, v) -> t.responseHeaders.set(k, v.toString()) }
        t.sendResponseHeaders(response.statusCode, response.data.size.toLong())
        val os = t.responseBody
        os.write(response.data)
        os.flush()
        os.close()
    })
    server.executor = null // creates a default executor
    server.start()
}

// SSL証明書チェックを外す(for Debug)
fun useInsecureSSL() {

    // Create a trust manager that does not validate certificate chains
    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate>? = null
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
    })

    val sc = SSLContext.getInstance("SSL")
    sc.init(null, trustAllCerts, java.security.SecureRandom())
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)

    // Create all-trusting host name verifier
    val allHostsValid = HostnameVerifier { _, _ -> true }

    // Install the all-trusting host verifier
    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
}

class MyHandler(val op: (t: HttpExchange) -> Unit) : HttpHandler {
    override fun handle(t: HttpExchange) {
        op(t)
    }
}

