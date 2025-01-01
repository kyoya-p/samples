import androidx.compose.runtime.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.cancel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val GOOGLE_CLIENT_ID = System.getenv("GOOGLE_CLIENT_ID")!!
val GOOGLE_CLIENT_SECRET = System.getenv("GOOGLE_CLIENT_SECRET")!!

val port = 28780

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
data class ErrorInfo(
    val error: String,
    val error_description: String,
)

@Composable
fun <T> suspendEffect(op: suspend () -> T): T? {
    var v by remember { (mutableStateOf<T?>(null)) }
    LaunchedEffect(Unit) { v = op() }
    return v
}

fun server(op: (code: String) -> Unit) = embeddedServer(io.ktor.server.cio.CIO, port = port) {
    routing {
        get("/") {
            call.respondText("${call.request.queryParameters["code"]}")
            op(call.request.queryParameters["code"] ?: return@get)
            cancel()
        }
    }
}

@Composable
fun googleAuth() {
    println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% googleAuth() %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
    var code by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { server { code = it }.start() }
    // Step 1: Get an authorization code
    val authorizationUrlQuery = parameters {
        append("client_id", GOOGLE_CLIENT_ID)
        append("scope", "https://www.googleapis.com/auth/userinfo.profile")
        append("response_type", "code")
        append("redirect_uri", "http://127.0.0.1:$port")
        append("access_type", "offline")
    }.formUrlEncode()
    println("https://accounts.google.com/o/oauth2/auth?$authorizationUrlQuery")
    val authorizationCode = code ?: return
    println("Authorization code: $authorizationCode")

    // Step 2: Create a storage for tokens
    val bearerTokenStorage by remember { mutableStateOf(mutableListOf<BearerTokens>()) }

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
                            append("client_id", GOOGLE_CLIENT_ID)
                            append("refresh_token", oldTokens?.refreshToken ?: "")
                        }
                    ) { markAsRefreshTokenRequest() }.body()
                    bearerTokenStorage.add(BearerTokens(refreshTokenInfo.access_token, oldTokens?.refreshToken!!))
                    bearerTokenStorage.last()
                }
                sendWithoutRequest { request -> request.url.host == "www.googleapis.com" }
            }
        }
    }
    var tokenInfoQ by remember { mutableStateOf<TokenInfo?>(null) }
    LaunchedEffect(Unit) {
        tokenInfoQ = client.submitForm(
            url = "https://accounts.google.com/o/oauth2/token",
            formParameters = parameters {
                append("grant_type", "authorization_code")
                append("code", authorizationCode)
                append("client_id", GOOGLE_CLIENT_ID)
                append("client_secret", GOOGLE_CLIENT_SECRET)
                append("redirect_uri", "http://127.0.0.1:$port")
            }
        ).body()
    }
    println(tokenInfoQ ?: return)
    val tokenInfo=tokenInfoQ?: return
    bearerTokenStorage.add(BearerTokens(tokenInfo.access_token, tokenInfo.refresh_token!!))

    val res = suspendEffect { client.get("https://www.googleapis.com/oauth2/v2/userinfo").bodyAsText() } ?: return
    val userInfo: UserInfo = Json.decodeFromString(res)
    println("Hello, ${res}!")
}
