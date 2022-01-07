import io.ktor.application.*
import io.ktor.network.tls.certificates.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.File

// https://proandroiddev.com/ssl-with-kotlin-and-ktor-61b3d7dccbc5

fun main() {
    val keyStoreFile = File("keystore.jks")
    val certAlias = "mycert"
    val storePass = "changeit"
    val certPass = "Soft2cream"

    val keystore = generateCertificate(
        file = keyStoreFile,
        keyAlias = "mycert",
        keyPassword = "Soft2cream",
        jksPassword = "changeit"
    )

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

