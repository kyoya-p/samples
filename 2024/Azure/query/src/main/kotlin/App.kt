import kotlinx.serialization.Serializable

@Serializable
data class AppData(
    val dbName: String = "",
    val query: String = "",
    val collName: String = "",
    val connStr: String = "",
    val result: MutableList<String> = mutableListOf(),
    val running: Boolean = false,
)

