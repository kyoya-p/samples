package com.example

import io.ktor.network.tls.certificates.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.tomcat.jakarta.*
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
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

    val secret = "changeit"
    val keyStoreFile = File("build/.keystore")

//    val keyStore = buildKeyStore {
//        certificate("tomcat") {
//            password = secret
//            domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
//            subject = X500Principal("CN=localhost,O=shokkaa,OU=shokkaa")
//        }
//    }
//    keyStore.saveToFile(keyStoreFile, secret)

    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()) // 通常は "JKS" または "PKCS12"
    FileInputStream(keyStoreFile).use { fis -> keyStore.load(fis, secret.toCharArray()) }

    connector {
        host = "0.0.0.0"
        port = 8080
    }

    sslConnector(
        keyStore = keyStore,
        keyAlias = "tomcat",
        keyStorePassword = { secret.toCharArray() },
        privateKeyPassword = { secret.toCharArray() }
    ) {
        port = 8443
        keyStorePath = keyStoreFile
    }
}

//fun makeCAcert() {
//    val keyStoreFile = File("ca.kts")
//    val keyStore = buildKeyStore {
//        certificate("ca") {
//            password = "changeit"
//            domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
//            subject = X500Principal("CN=localca,O=shokkaa,OU=shokkaa")
//        }
//    }
//    keyStore.saveToFile(keyStoreFile, "changeit")
//}