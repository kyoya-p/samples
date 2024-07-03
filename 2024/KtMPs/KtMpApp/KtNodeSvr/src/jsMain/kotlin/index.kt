import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.firestore.*
import dev.gitlive.firebase.initialize

external val process: dynamic

suspend fun main() {
    val args = (process.argv as Array<String>).drop(2)
    val appKey = process.env.APPKEY as String
    val (tg, pw) = args

    println("appKey=$appKey tg=$tg")

    val options = FirebaseOptions(
        apiKey = appKey,
        projectId = "road-to-iot",
        databaseUrl = "https://road-to-iot.firebaseio.com",
        applicationId = "1:307495712434:web:638b142c284daabef33bab",
    )

    val app = Firebase.initialize(Unit, options)
    val db = Firebase.firestore(app)
    db.settings = firestoreSettings(db.settings) { cacheSettings = memoryCacheSettings { } }

    val tgRef = db.collection("fireshell").document(tg)
    val d = tgRef.get().data<Map<String, String>>()
    println(d)

//    tgRef.collection("requests").add(mapOf("time" to now()))
//    val d = tgRef.collection("requests").limit(1).snapshots.first().documents[0].data<Map<String, String>>()
//    println(d)

    tgRef.collection("requests")
        .orderBy("time", Direction.ASCENDING)
        .where { "state" equalTo "" }
        .limit(10)
        .snapshots.collect {
            it.documents.forEach { execCommand(it.data<Map<String, String>>()) }
        }
}

external fun require(module: String): dynamic

val childProcess = require("child_process")
val execa = require("execa")

suspend fun execCommand(data: Map<String, String>) {
    val cmd = data["command"] ?: ""
    val args = (data["command"] ?: "").split(" ").toTypedArray()
    println(cmd)
//    val r = childProcess.spawn(cmd)
    val r = execa.execa(cmd).stdout
    println(r)
    val mData = data.toMutableMap()
    mData["state"] = "x"

}