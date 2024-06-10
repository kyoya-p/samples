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



fun query(app: AppData, filters: FilterList) = callbackFlow {
    MongoClient(MongoClientURI(app.connStr)).use { client ->
        client.getDatabase(app.dbName).getCollection(app.collName).apply {
            find(Filters.eq("type", "mfp")).forEach { doc -> trySendBlocking(doc!!) }
        }
        close()
    }
    awaitClose()
}
