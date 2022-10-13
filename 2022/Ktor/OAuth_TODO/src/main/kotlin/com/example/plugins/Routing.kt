package com.example.plugins

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.locations.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.request.*

fun Application.configureOAuth() {
    install(Authentication) {
    }
}

fun Application.configureRouting() {
    install(Locations) {
    }

    routing {
        get("/") {
            println("Server: / ${call.request.uri}")
            println("Server: / ${call.request.headers}")
            call.respondText("Hello World!")
        }
        get<MyLocation> {
            println("Server: MyLocation: ${call.request.uri}")
            println("Server: MyLocation: ${call.request.headers}")
            call.respondText("Location: name=${it.name}, arg1=${it.arg1}, arg2=${it.arg2}")
        }
        // Register nested routes
        get<Type.Edit> {
            println("Server: Type.Edit: ${call.request.uri}")
            println("Server: Type.Edit: ${call.request.headers}")
            call.respondText("Inside $it")
        }
        get<Type.List> {
            println("Server: Type.List: ${call.request.uri}")
            println("Server: Type.List: ${call.request.headers}")
            call.respondText("Inside $it")
        }
    }
}

@Location("/location/{name}")
class MyLocation(val name: String, val arg1: Int = 42, val arg2: String = "default")

@Location("/type/{name}")
data class Type(val name: String) {
    @Location("/edit")
    data class Edit(val type: Type)

    @Location("/list/{page}")
    data class List(val type: Type, val page: Int)
}
