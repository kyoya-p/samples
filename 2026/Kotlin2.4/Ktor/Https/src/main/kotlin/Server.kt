import io.ktor.network.tls.certificates.*
import io.ktor.network.tls.extensions.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import javax.security.auth.x500.X500Principal

val keyStore = buildKeyStore {
    certificate("sampleAlias") {
        password = "foobar"
        domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
        daysValid = 365
        keyType=KeyType.Server
        sign=SignatureAlgorithm.RSA
        subject= X500Principal("CN=127.0.0.1, OU=OrgUnit, O=Org, C=JP")
    }
}

fun main() {
    embeddedServer(Netty, applicationEngineEnvironment {
        sslConnector(
            keyStore = keyStore,
            keyAlias = "sampleAlias",
            keyStorePassword = { "123456".toCharArray() },
            privateKeyPassword = { "foobar".toCharArray() }
        ) { port = 8443 }
        connector { port = 8080 } // 非SSL用ポート
        module { myApplication() }
    }).start(wait = true)
}

fun Application.myApplication() = routing {
    get("/") { call.respondText("Hello World!") }
}
