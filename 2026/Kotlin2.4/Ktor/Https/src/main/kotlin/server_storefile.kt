import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// keystore (KEYSTORE_PATH、既定 build/storefiles/keystore.p12) を読み込んで起動する (certs.kt 参照)
//   第1引数に外部ファイルを指定すると、それを優先して読み込む (パスワードは KEYSTORE_PASS)
//   store file は mise run gen-stores (keytool) で再生成できる
// 注意: 形式は PKCS12 (JDK 9 以降の標準)
fun main(args: Array<String>) {
    val path = args.getOrNull(0) ?: KEYSTORE_PATH
    val keyStore = loadStore(path, KEYSTORE_PASS)
    println("keystore 読み込み: $path")

    embeddedServer(Netty, configure = {
        sslConnector(
            keyStore = keyStore,
            keyAlias = SERVER_ALIAS,
            keyStorePassword = { KEYSTORE_PASS.toCharArray() },
            privateKeyPassword = { KEYSTORE_PASS.toCharArray() }
        ) { port = 8443 }
        connector { port = 8080 } // 非SSL用ポート
    }) { myApplication() }.start(wait = true)
}

fun Application.myApplication() = routing {
    get("/") { call.respondText("Hello World!") }
}
