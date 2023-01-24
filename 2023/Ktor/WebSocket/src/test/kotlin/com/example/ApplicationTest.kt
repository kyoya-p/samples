package com.example

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import kotlin.test.*
import com.example.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureRouting()
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }

        val client = createClient {
            install(WebSockets)
        }
        client.webSocket("ws://localhost/ws") {
//            send("hello")
//            val r = incoming.receive().readBytes().toString()
//            this.send("bye")
//            close()
       }
    }
}
