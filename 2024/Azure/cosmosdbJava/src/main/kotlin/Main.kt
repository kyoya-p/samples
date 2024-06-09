import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.model.Filters
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
