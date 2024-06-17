import kotlinx.serialization.Serializable

@Serializable
data class AppData(
    val enabled: Boolean = false,
    val activationCode: String = "",
)
