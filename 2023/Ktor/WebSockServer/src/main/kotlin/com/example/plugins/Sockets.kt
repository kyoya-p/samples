package com.example.plugins

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.*

fun Application.configureSockets() {
    install(WebSockets) {
//        pingPeriod = Duration.ofSeconds(15)
//        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("/ws") {
            val r = call.request
            println("CONNECTED from:${r.origin.remoteHost}:${r.origin.remotePort}")
            println("URI: ${r.uri}")
            println("Headers:")
            r.headers.toMap().forEach { k, v -> println("  $k: $v") }
            for (frame in incoming) {
                println("frame:${frame.data}")
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    outgoing.send(Frame.Text("YOU SAID: $text"))
                    if (text.equals("bye", ignoreCase = true)) {
                        close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                    }
                }
            }
            println("CLOSED")
        }
    }
}
