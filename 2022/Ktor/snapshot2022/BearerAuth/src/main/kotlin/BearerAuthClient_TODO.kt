package bearerauthclient

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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class TokenInfo(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("scope") val scope: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("id_token") val idToken: String,
)


@Suppress("PLUGIN_IS_NOT_ENABLED")
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

fun main() = runBlocking {
    // ブラウザを使用して authorizationCode 取得
    val authorizationUrlQuery = Parameters.build {
        append("client_id", System.getenv("GOOGLE_CLIENT_ID"))
        append("scope", "https://www.googleapis.com/auth/userinfo.profile")
        append("response_type", "code")
        append("redirect_uri", "urn:ietf:wg:oauth:2.0:oob")
        append("access_type", "offline")
    }.formUrlEncode()
    println("https://accounts.google.com/o/oauth2/auth?$authorizationUrlQuery")
    println("Open a link above, get the authorization code, insert it below, and press Enter.")
    val authorizationCode = readln()
    val bearerTokenStorage = mutableListOf<BearerTokens>()

    // トークンを受け取り、保護されたAPIにアクセスするためのクライアント
    val client = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
        install(Auth) {
            bearer {
                loadTokens {
                    bearerTokenStorage.last()
                }
                refreshTokens {
                    val refreshTokenInfo: TokenInfo = client.submitForm(
                        url = "https://accounts.google.com/o/oauth2/token",
                        formParameters = Parameters.build {
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

    // 認証コードをトークンに交換し、トークンを保存する
    val tokenInfo: TokenInfo = client.submitForm(
        url = "https://accounts.google.com/o/oauth2/token",
        formParameters = Parameters.build {
            append("grant_type", "authorization_code")
            append("code", authorizationCode)
            append("client_id", System.getenv("GOOGLE_CLIENT_ID"))
            append("redirect_uri", "urn:ietf:wg:oauth:2.0:oob")
            // 帯域外(OOB)フロー(tokenコピペ)が廃止された(Google 2022/10/3)
            // https://developers.google.com/identity/protocols/oauth2/resources/oob-migration
            // https://developers.google.com/identity/protocols/oauth2/native-app#redirect-uri_loopback

        }
    ).body()
    bearerTokenStorage.add(BearerTokens(tokenInfo.accessToken, tokenInfo.refreshToken!!))

    val response: HttpResponse = client.get("https://www.googleapis.com/oauth2/v2/userinfo")
    val userInfo: UserInfo = response.body()
    println("Hello, ${userInfo.name}!")

    // 保護されたAPIにアクセス
    while (true) {
        println("Make a request? Type 'yes' and press Enter to proceed.")
        when (readln()) {
            "yes" -> {
                val response: HttpResponse = client.get("https://www.googleapis.com/oauth2/v2/userinfo")
                try {
                    val userInfo: UserInfo = response.body()
                    println("Hello, ${userInfo.name}!")
                } catch (e: Exception) {
                    val errorInfo: ErrorInfo = response.body()
                    println(errorInfo.error.message)
                }
            }

            else -> return@runBlocking
        }
    }
}

/*
* 参考:
* https://ktor.io/docs/bearer-client.htm
*
* OAuth2.0: https://developers.google.com/identity/protocols/oauth2/native-app
* OOB移行フロー: https://developers.google.com/identity/protocols/oauth2/resources/oob-migration
*/