import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.firestoreSettings
import dev.gitlive.firebase.firestore.memoryCacheSettings
import dev.gitlive.firebase.initialize

external val process: dynamic

suspend fun main() {
    val args = (process.argv as Array<String>).drop(2)
    val appKey = process.env.APPKEY as String
    val tg = args.getOrElse(0) { "default" }
    val pw = args.getOrElse(1) { "xxxxxxxx" }

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

    println("X1")
    db.collection("fireshell").add(mapOf("a" to "b"))
    val tgRef = db.collection("fireshell").document(tg)
    val d = tgRef.get().data<Map<String, String>>()
    tgRef.collection("requests").snapshots.collect {
        it.documents.forEach {
            println(it.id)
        }
    }

//    db.collection(tg).snapshots.collect { qs ->
//        qs.documents.map { println("[${it.id}]") }
//    }
    println(d)

}

