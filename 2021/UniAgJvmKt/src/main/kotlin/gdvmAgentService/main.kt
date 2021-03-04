package  gdvmAgentService

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.network.tls.certificates.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.websocket.*
import java.io.File

// Websocket:
// https://jp.ktor.work/servers/features/websockets.html


fun main(args: Array<String>) {
    val file = File("build/temporary.jks")
    if (!file.exists()) {
        file.parentFile.mkdirs()
        generateCertificate(file)
    }

    io.ktor.server.netty.EngineMain.main(args)
}  // application.confを読込む場合

@Suppress("unused")
fun Application.module() {
    routing {
        get("/") {
            call.respondText("First Sample", ContentType.Text.Html)
        }
    }
}

/*
fun main() {
    val file = File("build/temporary.jks")
    if (!file.exists()) {
        file.parentFile.mkdirs()
        generateCertificate(file)
    }
    val env = applicationEngineEnvironment {
        module {
            main()
        }
        sslConnector(keyStore = keyStore,keyAlias = "gdvmag",keyStorePassword = ){
            host="0.0.0.0"
            port=28086
        }
    }

    embeddedServer(Netty, port = 8000) {
        install(CORS) {
            method(HttpMethod.Options)
            header(HttpHeaders.XForwardedProto)
            anyHost()
            allowCredentials = true
            allowNonSimpleContentTypes = true
        }
        install(WebSockets)
        routing {
            get("/test") {
                call.respondText("Hello, World!")
            }
            webSocket("/") {
                println("onConnect")
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            outgoing.send(Frame.Text("YOU SAID: $text"))
                            if (text.equals("bye", ignoreCase = true)) {
                                close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                            }
                        }
                    }
                }
                println("onClosed")
            }
        }
    }.start(wait = true)
}


 */