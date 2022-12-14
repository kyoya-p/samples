package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import com.example.plugins.*
import com.apurebase.kgraphql.GraphQL

fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module(testing: Boolean = false) {
    install(GraphQL) {
        //configureRouting()
        playground = true
        schema {
            query("hello") {
                resolver { -> "GQL World" }
            }
        }
    }
}

