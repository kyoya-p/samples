import io.ktor.application.*
import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import io.ktor.network.tls.extensions.HashAlgorithm
import io.ktor.network.tls.extensions.SignatureAlgorithm
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.util.*
import java.io.File
import kotlin.text.toCharArray


// https://proandroiddev.com/ssl-with-kotlin-and-ktor-61b3d7dccbc5


fun main() {
    val keyStoreFile = File("build/temp.jks")
    val certAlias = "certificateAlias"
    val storePass = "1234xxxx"
    val keystore = buildKeyStore {
        certificate(certAlias) {
            hash = HashAlgorithm.SHA256
            sign = SignatureAlgorithm.ECDSA
            keySizeInBits = 256
            password = storePass
        }
    }
    keystore.saveToFile(keyStoreFile, storePass)

    fun appEnv(module: Application.() -> Unit) = applicationEngineEnvironment {
        sslConnector(keystore,
            certAlias,
            { "".toCharArray() },
            { storePass.toCharArray() }) {
            port = 8443
            keyStorePath = keyStoreFile.absoluteFile
            module(module)
        }
    }

    val server = embeddedServer(Netty, appEnv {
        routing {
            get("/") {
                call.respondText(
                    "HELLO ${call.request.queryParameters.toMap()}"
                )
            }
        }
    })
    println("Start Ktor Server port:${server.environment.connectors[0].port}")
    server.start(wait = true)
}
