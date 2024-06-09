import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.model.Filters
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import org.bson.Document

fun main(args: Array<String>) {
    MongoClient(MongoClientURI(args[0])).use { client ->
        client.getDatabase("db").getCollection("coll").apply {
            insertOne(Document("fruit", "apple"))
            insertOne(Document("fruit", "mango"))
            find(Filters.eq("fruit", "apple")).forEach { println(it.toJson()) }
        }
    }
}

fun query(app: AppData) = callbackFlow {
    MongoClient(MongoClientURI(app.connStr)).use { client ->
        client.getDatabase(app.dbName).getCollection(app.collName).apply {
            find(Filters.eq("type", "mfp")).forEach { doc -> trySendBlocking(doc!!) }
        }
        close()
    }
    awaitClose()
}