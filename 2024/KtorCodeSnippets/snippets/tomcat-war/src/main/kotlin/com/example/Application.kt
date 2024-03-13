package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import upload
import java.lang.Thread.currentThread
import java.time.Instant.now

fun Application.module() {
    routing {
        get("/") {
            var threadName = ""
            val t0 = now().toEpochMilli()
            runCatching {
                threadName = currentThread().name
                println("uploading: $threadName")
                upload()
            }.onFailure {
                println("onFailure")
                println(now().toEpochMilli() - t0)
                println(threadName)
                call.respondText(it.stackTraceToString())
            }.onSuccess {
                println("onSuccess")
                println(now().toEpochMilli() - t0)
                println(threadName)
                call.respondText("Hello,Thread:$threadName")
            }
        }
    }
}
