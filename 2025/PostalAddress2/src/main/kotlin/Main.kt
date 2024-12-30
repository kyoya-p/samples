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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenInfo(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val scope: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("id_token") val idToken: String,
)

@Serializable
data class UserInfo(
    val id: String,
    val name: String,
    @SerialName("given_name") val givenName: String,
    @SerialName("family_name") val familyName: String,
    val picture: String,
    val locale: String
)

@Serializable
data class ErrorInfo(val error: ErrorDetails)

@Serializable
data class ErrorDetails(
    val code: Int,
    val message: String,
    val status: String,
)

fun startServer() = embeddedServer(io.ktor.server.cio.CIO, port = 8080) {
    routing {
        get("/") {
            call.respondText("Authorization code: ${call.request.queryParameters["code"]}")
        }
    }
}.start()

suspend fun main() {
    startServer()

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
            install(ContentNegotiation) {
                json()
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        bearerTokenStorage.last()
                    }
                    refreshTokens {
                        val refreshTokenInfo: TokenInfo = client.submitForm(
                            url = "https://accounts.google.com/o/oauth2/token",
                            formParameters = parameters {
                                append("grant_type", "refresh_token")
                                append("client_id", System.getenv("GOOGLE_CLIENT_ID"))
                                append("refresh_token", oldTokens?.refreshToken ?: "")
                            }
                        ) { markAsRefreshTokenRequest() }.body()
                        bearerTokenStorage.add(BearerTokens(refreshTokenInfo.accessToken, oldTokens?.refreshToken!!))
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
        bearerTokenStorage.add(BearerTokens(tokenInfo.accessToken, tokenInfo.refreshToken!!))

        // Step 5: Make a request to the protected API
        while (true) {
            println("Make a request? Type 'yes' and press Enter to proceed.")
            when (readln()) {
                "yes" -> {
                    val response = client.get("https://www.googleapis.com/oauth2/v2/userinfo")
                    try {
                        val userInfo: UserInfo = response.body()
                        println("Hello, ${userInfo.name}!")
                    } catch (e: Exception) {
                        val errorInfo: ErrorInfo = response.body()
                        println(errorInfo.error.message)
                    }
                }
                else -> return
            }
        }
    }
