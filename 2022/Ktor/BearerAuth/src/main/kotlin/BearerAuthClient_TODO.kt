package bearerauthclient

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*

fun bearerAuthClient(user: String, password: String) = HttpClient(CIO) {
    install(Auth) {
        bearer {
            loadTokens { BearerTokens("abc123", "xyz111") }
            refreshTokens { BearerTokens("def456", "xyz111") }
        }
    }
}


/*
*  https://ktor.io/docs/bearer-client.html
*
*/