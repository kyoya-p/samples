import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.JsonObject
import okio.Path.Companion.toPath

fun listDocument() {
    val connStr = System.getenv("CONNSTR")
    val client = MongoClient(MongoClientURI(connStr))
    val mfps = client.findDocuments("rmmdb", "deviceLatest", listOf("type=mfp"))
    val dpys = client.findDocuments("rmmdb", "deviceLatest", listOf("type=display"))
    val outFile = "output.csv".toPath().toFile()
    outFile.writeText("model,sn,tenant,type\n")
    (mfps + dpys).map { decodeFromString<JsonObject>(it.toJson()) }.forEach {
        val m = (it["deviceGeneral"] as JsonObject)["modelName"]
        val s = (it["deviceGeneral"] as JsonObject)["serialNumber"]
        val g = it["relatedGroupId"]
        val t = it["type"]
        val row = "$m,$s,$g,$t\n"
        print(row)
        outFile.appendText(row)
    }
    client.close()
}

