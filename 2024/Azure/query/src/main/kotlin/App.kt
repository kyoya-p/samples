import kotlinx.serialization.Serializable

@Serializable
data class AppData(
    val dbName: String = "",
    val collName: String = "",
    val connStr: String = "",
    val result: MutableList<String> = mutableListOf(),
    val running: Boolean = false,
)

@Serializable
data class Filter(
    val field: String,
    val value: String,
    val operator: String,
)
typealias FilterList = List<Filter>
