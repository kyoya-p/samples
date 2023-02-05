package com.example

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import kotlin.test.*
import com.example.plugins.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest

class ApplicationTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun ktorTest() = runTest {
        val svr = embeddedServer(CIO, port = 80, host = "0.0.0.0", module = Application::module).start()

        val client = HttpClient { install(WebSockets) }
        client.webSocket("/ws") {
            send("hello")
            val r = incoming.receive() as? Frame.Text
            println(r?.readText())
            this.send("bye")
        }
        client.close()
        println("client closed.")
        svr.stop()
    }
}
