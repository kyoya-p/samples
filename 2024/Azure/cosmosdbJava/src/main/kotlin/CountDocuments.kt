import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq

fun countDocuments(connStr: String) {
    val client = MongoClient(MongoClientURI(connStr))
    val collGroup = client.getDatabase("rmmdb").getCollection("groupCollection")
    val groups = collGroup.find(eq("groupType", "Cluster")).filterNotNull().map { it.getString("groupId")!! }
    val docs = client.getDatabase("rmmdb").getCollection("deviceLatest")
    fun findDoc(groupId: String, type: String) = docs.find(and(eq("type", type), eq("relatedGroupId", groupId)))
    fun countDoc(groupId: String, type: String) = findDoc(groupId, type).count()
    val sMfp = groups.sumOf { groupId -> countDoc(groupId, "mfp").also { println("$groupId, mfp, $it") } }
    val sDpy = groups.sumOf { groupId -> countDoc(groupId, "display").also { println("$groupId, dpy, $it") } }
    println("Total, mfp, $sMfp")
    println("Total, dpy, $sDpy")
    client.close()
}
