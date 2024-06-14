import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq

const val topGroupId = "0"

fun countDevice(connStr: String) {
    val client = MongoClient(MongoClientURI(connStr))
    val collGroup = client.getDatabase("rmmdb").getCollection("groupCollection")
    val groups = collGroup.find(eq("groupType", "Cluster")).filterNotNull().map { it.getString("groupId")!! }
    val collDevice = client.getDatabase("rmmdb").getCollection("deviceLatest")
    fun devices(groupId: String, type: String) = collDevice.find(and(eq("type", type), eq("relatedGroupId", groupId)))
    val s=groups.map { groupId ->
        val nMfp = devices(groupId, "mfp").count()
        val nDsp = devices(groupId, "display").count()
        println("$groupId: mfp=$nMfp dpy=$nDsp total=${nMfp + nDsp}")
        nMfp + nDsp
    }.sum()
    println(s)
    client.close()
}
