import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.model.Filters
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectIndexed

suspend fun main(args: Array<String>) = callbackFlow {
    val connStr = System.getenv("CONNSTR")!!
    val (db, collection) = args
    val filters = args.drop(3).map { it.split("=") }.map { (f, v) -> Filters.eq(f, v) }
    MongoClient(MongoClientURI(connStr)).use { client ->
        client.getDatabase(db).getCollection(collection).apply {
            when (filters.isEmpty()) {
                true -> find()
                else -> find(Filters.and(filters))
            }.forEach { doc -> trySendBlocking(doc!!) }
        }
        close()
    }
    awaitClose()
}.collectIndexed { i, it -> println("$i: ${it.toJson().take(128)}") }

