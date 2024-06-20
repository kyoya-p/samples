import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq

fun findDocuments(connStr: String, db: String, collName: String, filters: List<String>) {
    val client = MongoClient(MongoClientURI(connStr))
    client.findDocuments(db, collName, filters).forEachIndexed { i, e ->
        println("$i: ${e.toJson()}")
    }
    client.close()
}

fun MongoClient.findDocuments(db: String, collName: String, filters: List<String>) =
    getDatabase(db).getCollection(collName).run {
        val filter = and(filters.map { it.split("=") }.map { (f, v) -> println("Filter: $f=$v");eq(f, v) })
        val query = if (filters.isEmpty()) find() else find(filter)
        query.filterNotNull().asSequence()
    }

