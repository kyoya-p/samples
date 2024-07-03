import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import org.bson.Document


fun main(args: Array<String>) {
    val (dbName, collName, sn) = args
    val uri = MongoClientURI(System.getenv("CONNSTR"))
    println("URL: $uri")
    MongoClient(uri).use { mongoClient ->
        println("Database.Collection: $dbName.$collName")
        val database = mongoClient.getDatabase(dbName)
        val collection = database.getCollection(collName)

        println("Query: {serialNumber:$sn}")
        val query = Filters.eq("serialNumber", sn)
        val queryResults = collection.find(query).limit(1)
        queryResults.forEachIndexed { i, e -> println("[$i] ${e.toJson()}") }
    }
}

// Sample code
@Suppress("unused")
fun main2() {
    val uri = MongoClientURI(System.getenv("CONNSTR"))
    var mongoClient: MongoClient? = null
    try {
        mongoClient = MongoClient(uri)
        val database: MongoDatabase = mongoClient.getDatabase("db")
        val collection: MongoCollection<Document> = database.getCollection("coll")
        val document1 = Document("fruit", "apple")
        collection.insertOne(document1)
        val document2 = Document("fruit", "mango")
        collection.insertOne(document2)

        val queryResults = collection.find(Filters.eq("fruit", "apple"))
        queryResults.forEachIndexed { i, e -> println("[$i] ${e.toJson()}") }
        println("Completed successfully")
    } finally {
        mongoClient?.close()
    }
}
