import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.cancel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun main() {
    val client = HttpClient(CIO)
    val redirectUri = "http://127.0.0.1:28080"
    val authorizationUrl = URLBuilder("https://accounts.google.com/o/oauth2/auth").apply {
        parameters.append("client_id", System.getenv("GOOGLE_CLIENT_ID")!!)
        parameters.append("redirect_uri", redirectUri)
        parameters.append("response_type", "code")
        parameters.append("scope", "https://www.googleapis.com/auth/userinfo.profile")
    }.buildString()
    println("ブラウザで以下のURLを開いて認証してください:\n$authorizationUrl")
    // 上で表示されるURLを利用者が開いたブラウザに入力し認証操作を続ける

    val authorizationCode = redirectReceiverServer()!! // ここで認証(リダイレクト)を待っている
    println("authorizationCode=$authorizationCode")

    val tokenResponse = client.submitForm(
        url = "https://accounts.google.com/o/oauth2/token",
        formParameters = Parameters.build {
            append("grant_type", "authorization_code")
            append("code", authorizationCode)
            append("redirect_uri", redirectUri)
            append("client_id", System.getenv("GOOGLE_CLIENT_ID")!!)
            append("client_secret", System.getenv("GOOGLE_CLIENT_SECRET")!!)
        }
    ).bodyAsText()
    println("accessToken:\n$tokenResponse")
    client.close()
}

suspend fun redirectReceiverServer() = suspendCoroutine { ctn ->
    embeddedServer(io.ktor.server.cio.CIO, port = 28080) {
        routing {
            get("/") {
                call.respondText("認証完了しました")
                ctn.resume(call.request.queryParameters["code"])
                cancel()
            }
        }
    }.start()
}

