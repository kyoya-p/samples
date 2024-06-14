import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.model.Filters
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import okio.Path.Companion.toPath

suspend fun main(args: Array<String>) {
    val connStr = System.getenv("CONNSTR")!!
    val (db) = args
    MongoClient(MongoClientURI(connStr)).use { client ->
        val collGroup        client.getDatabase(db).getCollection("groupCollection")
    }
}


val filters = args.drop(2).map { it.split("=") }.map { (f, v) -> println("Filter: $f=$v");Filters.eq(f, v) }
MongoClient(MongoClientURI(connStr)).use {
    client ->
    client.getDatabase(db).getCollection(collection).apply {
        val query = if (filters.isEmpty()) find() else find(Filters.and(filters))
        query.forEach { doc -> trySendBlocking(doc!!) }
    }
    close()
}
awaitClose()
}.withIndex().map { (i, doc) -> doc.toJson()!!.also { println("$i: ${it.take(128)}") } }.toList().let {
    docs ->
    val jsonList = docs.map { Json.decodeFromString<JsonObject>(it) }
    "output.json".toPath().toFile().writeText(Json.encodeToString(jsonList))
}

