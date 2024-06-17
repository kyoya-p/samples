import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.model.Filters
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.Serializable

@Serializable
data class AppData(
    val dbName: String = "",
    val collName: String = "",
    val connStr: String = "",
    val result: List<String> = listOf(),
    val running: Boolean = false,
)

@Serializable
data class Filter(
    val field: String,
    val value: String,
    val operator: String = "eq",
)
typealias FilterList = List<Filter>

@Serializable
data class WinSize(val width:Int, val height:Int)

fun query(app: AppData, filters: FilterList) = callbackFlow {
    MongoClient(MongoClientURI(app.connStr)).use { client ->
        fun FilterList.build() = Filters.and(map { Filters.eq(it.field, it.value) })
        client.getDatabase(app.dbName).getCollection(app.collName).apply {
            when (filters.isEmpty()) {
                true -> find().forEach { doc -> trySendBlocking(doc!!) }
                else -> find(filters.build()).forEach { doc -> trySendBlocking(doc!!) }
            }
        }
        close()
    }
    awaitClose()
}

suspend fun main(args: Array<String>) {
    val app = AppData(
        connStr = args[0],
        dbName = "rmmdb",
        collName = "deviceLatest",
    )
    val filters = listOf(Filter("type", "mfp", "eq"))
    query(app, filters).collect {
        println(it.toJson())
    }
}