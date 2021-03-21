import io.ktor.application.*
import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import io.ktor.network.tls.extensions.HashAlgorithm
import io.ktor.network.tls.extensions.SignatureAlgorithm
import io.ktor.server.engine.*
import io.ktor.server.netty.Netty
import io.ktor.util.*
import java.io.File
import java.security.KeyStore
import kotlin.text.toCharArray

// https://proandroiddev.com/ssl-with-kotlin-and-ktor-61b3d7dccbc5


@KtorExperimentalAPI
fun main() {
    val keyStoreFile = File("keystore.jks")
    val certAlias = "mycert"
    val storePass = "changeit"
    val certPass = "mycert"
    val keystore = if (keyStoreFile.exists()) {
        KeyStore.getInstance(keyStoreFile, storePass.toCharArray())
    } else {
        buildKeyStore {
            certificate(certAlias) {
                hash = HashAlgorithm.SHA256
                sign = SignatureAlgorithm.ECDSA
                keySizeInBits = 256
                password = storePass
            }
        }.apply { saveToFile(keyStoreFile, storePass) }
    }

    fun appEnv(module: Application.() -> Unit) = applicationEngineEnvironment {
        connector { port = 80 }
        sslConnector(
            keyStore = keystore,
            keyAlias = certAlias,
            keyStorePassword = { storePass.toCharArray() },
            privateKeyPassword = { certPass.toCharArray() }) {
            port = 443
            keyStorePath = keyStoreFile.absoluteFile
        }
        module(module)
    }

    val server = embeddedServer(Netty, appEnv(appWebsocket))
    println("Start Ktor Server port:${server.environment.connectors[0].port} sslPort:${server.environment.connectors[1].port}")
    server.start(wait = true)
}
