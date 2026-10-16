import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

// 証明書検証あり版。truststore (TRUSTSTORE_PATH) の CA 証明書を参照し、その CA が署名した証明書だけを信頼する
// (curl の --cacert と同じ考え方。truststore は mise run gen-stores で生成する)
// 先に Server を起動しておくこと
fun trustManagerOf(path: String, pass: String): X509TrustManager {
    val ks = loadStore(path, pass)
    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(ks) }
    return tmf.trustManagers.filterIsInstance<X509TrustManager>().first()
}

suspend fun main(args: Array<String>) {
    // 引数で truststore を指定可 (パスワードは TRUSTSTORE_PASS)。既定は TRUSTSTORE_PATH
    val trustFile = args.getOrNull(0) ?: TRUSTSTORE_PATH
    val client = HttpClient(CIO)
    println("http:// ${client.get("http://localhost:8080").bodyAsText()}")

    val clientHttps = HttpClient(CIO) {
        engine {
            https {
                trustManager = trustManagerOf(trustFile, TRUSTSTORE_PASS)
            }
        }
    }
    println("https:// ${clientHttps.get("https://localhost:8443").bodyAsText()}")

    // 既定の信頼ストアのままだと、自前 CA の証明書は拒否される
    val clientDefault = HttpClient(CIO)
    val rejected = runCatching { clientDefault.get("https://localhost:8443").bodyAsText() }
    println("https:// (既定の信頼ストア) ${rejected.fold({ it }, { "拒否: ${it::class.simpleName}: ${it.message}" })}")

    client.close()
    clientHttps.close()
    clientDefault.close()
}
