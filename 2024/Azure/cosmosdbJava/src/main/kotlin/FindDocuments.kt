import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath

fun findDocuments(connStr: String, db: String, collName: String, filters: List<String>) {
    val filter = and(filters.map { it.split("=") }.map { (f, v) -> println("Filter: $f=$v");eq(f, v) })
    val client = MongoClient(MongoClientURI(connStr))
    client.getDatabase(db).getCollection(collName).apply {
        val query = if (filters.isEmpty()) find() else find(filter)
        val devs = query.map { it.toJson() }.onEachIndexed { i, doc -> println("$i: ${doc.take(128)}") }
        "output.json".toPath().toFile().writeText(Json.encodeToString(devs))
    }
    client.close()
}

