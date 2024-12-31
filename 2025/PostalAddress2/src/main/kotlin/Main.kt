//@file:Suppress("PLUGIN_IS_NOT_ENABLED")

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds


@Serializable
data class TokenInfo(
    val access_token: String,
    val expires_in: Int,
    val refresh_token: String? = null,
    val scope: String,
    val token_type: String,
    val id_token: String,
)

@Serializable
data class UserInfo(
    val id: String,
    val name: String,
    val given_name: String,
    val family_name: String,
    val picture: String,
)

@Serializable
data class ErrorInfo(val error: ErrorDetails)

@Serializable
data class ErrorDetails(
    val code: Int,
    val message: String,
    val status: String,
)

fun startRedirectReceiverServer() = embeddedServer(io.ktor.server.cio.CIO, port = 8080) {
    routing {
        get("/") {
            call.respondText("Authorization code: ${call.request.queryParameters["code"]}")
        }
    }
}.start()

suspend fun main() {
    startRedirectReceiverServer()
    val tokenOriginTime by lazy { now() }
    // Step 1: Get an authorization code
    val authorizationUrlQuery = parameters {
        append("client_id", System.getenv("GOOGLE_CLIENT_ID"))
        append("scope", "https://www.googleapis.com/auth/userinfo.profile")
        append("response_type", "code")
        append("redirect_uri", "http://127.0.0.1:8080")
        append("access_type", "offline")
    }.formUrlEncode()
    println("https://accounts.google.com/o/oauth2/auth?$authorizationUrlQuery")
    println("Open a link above, get the authorization code, insert it below, and press Enter.")
    val authorizationCode = readln()

    // Step 2: Create a storage for tokens
    val bearerTokenStorage = mutableListOf<BearerTokens>()

    // Step 3: Configure the client for receiving tokens and accessing the protected API
    val client = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
        install(Auth) {
            bearer {
                loadTokens { bearerTokenStorage.last() }
                refreshTokens {
                    val refreshTokenInfo: TokenInfo = client.submitForm(
                        url = "https://accounts.google.com/o/oauth2/token",
                        formParameters = parameters {
                            append("grant_type", "refresh_token")
                            append("client_id", System.getenv("GOOGLE_CLIENT_ID"))
                            append("refresh_token", oldTokens?.refreshToken ?: "")
                        }
                    ) { markAsRefreshTokenRequest() }.body()
                    bearerTokenStorage.add(BearerTokens(refreshTokenInfo.access_token, oldTokens?.refreshToken!!))
                    bearerTokenStorage.last()
                }
                sendWithoutRequest { request ->
                    request.url.host == "www.googleapis.com"
                }
            }
        }
    }

    // Step 4: Exchange the authorization code for tokens and save tokens in the storage
    val tokenInfo: TokenInfo = client.submitForm(
        url = "https://accounts.google.com/o/oauth2/token",
        formParameters = parameters {
            append("grant_type", "authorization_code")
            append("code", authorizationCode)
            append("client_id", System.getenv("GOOGLE_CLIENT_ID"))
            append("client_secret", System.getenv("GOOGLE_CLIENT_SECRET"))
            append("redirect_uri", "http://127.0.0.1:8080")
        }
    ).body()
    println(tokenOriginTime.toLocalDateTime(TimeZone.currentSystemDefault()))
    println(tokenInfo)
    bearerTokenStorage.add(BearerTokens(tokenInfo.access_token, tokenInfo.refresh_token!!))

    // Step 5: Make a request to the protected API
    while (true) {
        println("Make a request? Type 'yes' and press Enter to proceed.")
        println("Expire: ${(tokenOriginTime + tokenInfo.expires_in.seconds).toLocalDateTime(TimeZone.currentSystemDefault())}")
        when (readln()) {
            "yes" -> {
                val res: String = client.get("https://www.googleapis.com/oauth2/v2/userinfo").body()
                try {
                    val userInfo: UserInfo = Json.decodeFromString(res)
                    println("Hello, ${res}!")
                } catch (e: Exception) {
                    val errorInfo: ErrorInfo = Json.decodeFromString(res)
                    println(errorInfo.error.message)
                }
            }
            else -> return
        }
    }
}
