import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.model.Filters
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import org.bson.Document

suspend fun main(args: Array<String>) = callbackFlow {
    MongoClient(MongoClientURI(args[0])).use { client ->
        client.getDatabase("db").getCollection("coll").apply {
            insertOne(Document("fruit", "apple"))
            insertOne(Document("fruit", "mango"))
            find(Filters.eq("fruit", "apple")).forEach { doc -> trySendBlocking(doc!!) }
        }
        close()
    }
    awaitClose()
}.collect { println(it.toJson()) }

