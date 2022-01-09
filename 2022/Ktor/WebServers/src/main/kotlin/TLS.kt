import io.ktor.application.*
import io.ktor.http.*
import io.ktor.network.tls.certificates.*
import io.ktor.network.tls.extensions.HashAlgorithm
import io.ktor.network.tls.extensions.SignatureAlgorithm
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.Netty
import io.ktor.util.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.text.toCharArray

// https://proandroiddev.com/ssl-with-kotlin-and-ktor-61b3d7dccbc5


fun main() {
    val keyStoreFile = File("build/keystore.jks")
    val certAlias = "mycert"
    val storePass = "changeit"
    val certPass = "changeit"

    val keystore = generateCertificate(
        file = keyStoreFile,
        keyAlias = certAlias,
        keyPassword = certPass,
        jksPassword = storePass
    )

    fun appEnv(module: Application.() -> Unit) = applicationEngineEnvironment {
        connector { port = 8080 } // for HTTP
        sslConnector(
            keyStore = keystore,
            keyAlias = certAlias,
            keyStorePassword = { storePass.toCharArray() },
            privateKeyPassword = { certPass.toCharArray() }
        ) {
            port = 8443  // for HTTPS
            keyStorePath = keyStoreFile.absoluteFile
        }
        module(module)
    }

    embeddedServer(Netty, appEnv {
        routing {
            get("/") {
                println("get/")
                val h = Json { prettyPrint = true }.encodeToString(call.request.headers.toMap())
                call.respondText("Hello Get Request! \n$h", ContentType.Text.Plain)
            }
        }
    }).start(wait = true)
}

