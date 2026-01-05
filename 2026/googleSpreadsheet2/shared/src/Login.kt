import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlin.coroutines.resume

suspend fun login(client: HttpClient, clientId: String, clientSecret: String): TokenResponse {
    val redirectUri = "http://127.0.0.1:28080"
    val authorizationUrl = URLBuilder("https://accounts.google.com/o/oauth2/auth").apply {
        parameters.append("client_id", clientId)
        parameters.append("redirect_uri", redirectUri)
        parameters.append("response_type", "code")
        parameters.append("scope", "https://www.googleapis.com/auth/userinfo.profile")
    }.buildString()
    println("Open the following URL in your browser and authenticate:\n$authorizationUrl")
    val authorizationCode = redirectReceiverServer()!!

//    val client = HttpClient(CIO) { install(ContentNegotiation) { json() } }
    val tokenResponse = client.submitForm(
        formParameters = Parameters.build {
            append("grant_type", "authorization_code")
            append("code", authorizationCode)
            append("redirect_uri", redirectUri)
            append("client_id", clientId)
            append("client_secret", clientSecret)
        },
        url = "https://accounts.google.com/o/oauth2/token"
    ).body<TokenResponse>()
    println("accessToken:\n${tokenResponse.access_token}")
    println("refreshToken:\n${tokenResponse.refresh_token}")
    client.close()
    return tokenResponse
}

suspend fun redirectReceiverServer() = suspendCancellableCoroutine { ctn ->
    embeddedServer(io.ktor.server.cio.CIO, port = 28080) {
        routing {
            get("/") {
                call.respondText("認証完了しました")
                ctn.resume(call.request.queryParameters["code"])
            }
        }
    }.start()
}

@Serializable
data class TokenResponse(
    val access_token: String,
    val expires_in: Int,
    val refresh_token: String,
    val scope: String,
    val token_type: String,
    val id_token: String,
)
