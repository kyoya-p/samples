import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.firestore.*
import dev.gitlive.firebase.firestore.Timestamp.Companion.now
import dev.gitlive.firebase.initialize

external val process: dynamic

suspend fun main() {
    val args = (process.argv as Array<String>).drop(2)
    val appKey = process.env.APPKEY as String
    val tg = args.getOrElse(0) { "default" }
    val pw = args.getOrElse(1) { secret }

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
    tgRef.collection("requests").add(mapOf("time" to now()))

//    val d = tgRef.collection("requests").limit(1).snapshots.first().documents[0].data<Map<String, String>>()
//    println(d)

    tgRef.collection("requests")
        .orderBy("time")
        .where { "state" equalTo "1"  }
        .limit(10)
        .snapshots.collect {
            it.documents.forEach { println(it.data<Map<String, String>>()) }
        }

//    db.collection(tg).snapshots.collect { qs ->
//        qs.documents.map { println("[${it.id}]") }
//    }

}

