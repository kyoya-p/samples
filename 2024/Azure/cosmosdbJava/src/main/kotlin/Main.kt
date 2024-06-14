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

fun main(args: Array<String>) {
    val connStr = System.getenv("CONNSTR")
    when (args[0]) {
        "countTenantDevice" -> countDevice(connStr)
        else -> countCollection(connStr, db = args[0], collName = args[1], filters = args.drop(2))
    }
}

