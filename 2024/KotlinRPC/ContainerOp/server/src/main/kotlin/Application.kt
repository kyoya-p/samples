package jp.wjg.shokkaa.container

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.cio.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import kotlinx.rpc.krpc.ktor.server.RPC
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import java.io.File

fun main(args: Array<String>): Unit = EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    install(RPC)
    installCORS()
    routing {
        rpc("/api") {
            rpcConfig { serialization { json() } }
            registerService<UserService> { ctx -> UserServiceImpl(ctx) }
        }
        staticFileSystem("/", "./") {
            println("Current Path: ${File(".").absolutePath}")
        }
    }
}

fun Application.installCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeader(HttpHeaders.Upgrade)
        allowNonSimpleContentTypes = true
        allowCredentials = true
        allowSameOrigin = true

        // webpack-dev-server and local development
      	//val allowedHosts = listOf("localhost:3000", "localhost:8080", "localhost:8081", "127.0.0.1:8080")
        val allowedHosts = listOf("*")
        allowedHosts.forEach { host ->
            allowHost(host, listOf("http", "https", "ws", "wss"))
        }
    }
}
