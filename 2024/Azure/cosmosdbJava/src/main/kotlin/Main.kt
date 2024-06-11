import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.model.Filters
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*


suspend fun main(args: Array<String>) = callbackFlow {
    val connStr = System.getenv("CONNSTR")!!
    val (db, collection) = args
    val filters = args.drop(2).map { it.split("=") }.map { (f, v) -> println("Filter: $f=$v");Filters.eq(f, v) }
    MongoClient(MongoClientURI(connStr)).use { client ->
        client.getDatabase(db).getCollection(collection).apply {
            val query = if (filters.isEmpty()) find() else find(Filters.and(filters))
            query.forEach { doc -> trySendBlocking(doc!!) }
        }
        close()
    }
    awaitClose()
}.withIndex().collect { (i, doc) -> println("$i: ${doc.toJson().take(128)}") }

