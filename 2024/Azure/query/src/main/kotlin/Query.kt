import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.model.Filters
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow

fun query(app: AppData) = callbackFlow {
    MongoClient(MongoClientURI(app.connStr)).use { client ->
        client.getDatabase(app.dbName).getCollection(app.collName).apply {
            find(Filters.eq("type", "mfp")).forEach { doc -> trySendBlocking(doc!!) }
        }
        close()
    }
    awaitClose()
}