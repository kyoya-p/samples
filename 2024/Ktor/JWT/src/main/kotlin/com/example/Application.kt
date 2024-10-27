package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
//    configureSerialization()
//    configureSecurity()
//    configureRouting()

    val jwtAudience = "jwt-audience"
    val jwtDomain = "https://jwt-provider-domain/"
    val jwtSecret = "secret0123456789"

    authentication {
        jwt("jwt1") {
            realm = "ktor sample app"
            verifier(JWT.require(Algorithm.HMAC256(jwtSecret)).build())
            validate {
                val uid = it.payload.getClaim("uid").asString()
                println("token.uid=$uid")
            }
        }
    }
    routing {
        get("/login/{uid}") {
            val uid = call.parameters["uid"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val token = JWT.create().withClaim("uid", uid).sign(Algorithm.HMAC256(jwtSecret))
            call.response.headers.append(HttpHeaders.Authorization, "Bearer $token")
            call.respondRedirect("/")
        }
        authenticate("jwt1") {
            get("/") { call.respondText("Hello World!") }
        }
    }

}
