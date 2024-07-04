import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.firestore.*
import dev.gitlive.firebase.initialize
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

external val process: dynamic

val options = FirebaseOptions(
    apiKey = process.env.APPKEY as String,
    projectId = "road-to-iot",
    databaseUrl = "https://road-to-iot.firebaseio.com",
    applicationId = "1:307495712434:web:638b142c284daabef33bab",
)

val app = Firebase.initialize(Unit, options)
val db = Firebase.firestore(app).apply {
    settings = firestoreSettings(settings) { cacheSettings = memoryCacheSettings { } }
}

suspend fun main() {
    val args = (process.argv as Array<String>).drop(2)
    val (tg, pw) = args
    val tgRef = db.collection("fireshell").document(tg)

    tgRef.collection("requests")
        .orderBy("time", Direction.ASCENDING)
        .where { "state" equalTo "" }
        .limit(10)
        .snapshots.collect {
            it.documents.forEach { execCommand(it) }
        }
}


suspend fun execCommand(req: DocumentSnapshot) {
    val data = req.data<Map<String, String>>()
    println(data)
    val r0 = execExecia<dynamic>(data["command"]!!) //.await()
    println("X1")
    val r = r0.await()
//    val mData = data.toMutableMap()
    println(r.stdout)
//    println(r.exitCode)

//    mData.set("exitCode", r.exitCode)
//    mData.set("stdout", r.stdout)
//    req.reference.set(mData)
}

// Javascript inter-op

suspend fun <T> Promise<T>.await(): T = suspendCoroutine { cont ->
    then({ println("L1");cont.resume(it) })
    catch { println("L2"); cont.resumeWithException(it) }
}

external fun require(module: String): dynamic

@Suppress("UNUSED_VARIABLE")
fun <T> execExecia(run: String): Promise<T> {
    val execa = require("execa")

    val (cmd, args) = run.split(" ").let { it.first() to it.drop(1).toTypedArray() }
    println(cmd)
    println(args)
    println("E0")
//    val res = js("execa(cmd,args);")
    val res = execa(cmd, args)
    println("E1")
    return res
}