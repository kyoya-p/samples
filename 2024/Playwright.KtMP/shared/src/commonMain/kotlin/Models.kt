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

