package com.example

import io.ktor.network.tls.certificates.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.tomcat.jakarta.*
import java.io.File
import javax.security.auth.x500.X500Principal

fun main() {
    embeddedServer(
        factory = Tomcat,
        environment = applicationEnvironment { },
        configure = { envConfig() },
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureSecurity()
    configureRouting()
}

private fun ApplicationEngine.Configuration.envConfig() {

    val keyStoreFile = File(".keystore")
    val keyStore = buildKeyStore {
        certificate("tomcat") {
            password = "changeit"
            domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
            subject = X500Principal("CN=localhost,O=shokkaa,OU=shokkaa")
        }
    }

    keyStore.saveToFile(keyStoreFile, "changeit")

    connector { port = 8080 }
    sslConnector(
        keyStore = keyStore,
        keyAlias = "tomcat",
        keyStorePassword = { "changeit".toCharArray() },
        privateKeyPassword = { "changeit".toCharArray() }) {
        port = 8443
        keyStorePath = keyStoreFile
    }
}

fun makeCAcert() {
    val keyStoreFile = File("ca.kts")
    val keyStore = buildKeyStore {
        certificate("ca") {
            password = "changeit"
            domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
            subject = X500Principal("CN=localca,O=shokkaa,OU=shokkaa")
        }
    }
    keyStore.saveToFile(keyStoreFile, "changeit")
}