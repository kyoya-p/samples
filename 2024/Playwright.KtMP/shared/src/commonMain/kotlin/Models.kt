import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed class Request {
    @Serializable
    data class LoginReq(val user: String? = null, val password: String? = null) : Request()

    @Serializable
    data object Terminate : Request()
}

fun main() {
    val reqLogin: Request.LoginReq = Request.LoginReq("shokkaa", "***")
    val reqTerm: Request.Terminate = Request.Terminate
    val x: Request = reqLogin
    val y: Request = reqTerm
    val x1 = Json.encodeToString(x) // {"type":"Request.LoginReq","user":"shokkaa","password":"***"}
    val y1 = Json.encodeToString(y) // {"type":"Request.Terminate"}
    Json.decodeFromString<Request>(x1) // LoginReq(user=shokkaa, password=***)
    Json.decodeFromString<Request>(y1) // Terminate
    runCatching {
        Json.decodeFromString<Request>("""{"user":"shokkaa","password":"***"}""")
    }.onSuccess { throw Exception("Expected Failure") }
    Json.decodeFromString<Request.LoginReq>("""{"user":"shokkaa","password":"***"}""").also(::println)
}