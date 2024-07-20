import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.firestore.*
import dev.gitlive.firebase.initialize
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


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

external fun require(module: String): dynamic

suspend fun main() = runCatching {
//  val args = (process.argv as Array<String>).drop(2)
//  val (tg, pw) = args
    val tg = process.env.TARGETID as String
    println("TARGETID=$tg")

    val refRqs = db.collection("fireshell").document(tg).collection("requests")

    refRqs.orderBy("time", Direction.ASCENDING).where { "isComplete" notEqualTo true }.limit(10).snapshots.collect {
        it.documents.forEach { ds ->
            val req = ds.data<Request>()
            runCatching {
                println("Run: ${req.cmd}")
                val res = spawn(req.cmd)
                ds.reference.set<Request>(req.copy(isComplete = true, result = res))
            }.onFailure {
                ds.reference.set<Request>(req.copy(isComplete = true, exception = it.message))
            }
        }
    }
}.onFailure { println(it.stackTraceToString()) }.getOrElse { }

val child_process = require("child_process")

suspend fun spawn(cmdLine: String) = suspendCoroutine { cont ->
    runCatching {
        var r = 0
        val (cmd, args) = cmdLine.split(" ").let { it.first() to it.drop(1).toTypedArray() }
        val ls = child_process.spawn(cmd, args)
        val stdout = ArrayDeque<String>()
        val stderr = ArrayDeque<String>()
        ls.stdout.on("data") { data -> stdout.add("$data".take(80));while (stdout.size > 5) stdout.removeFirst() }
        ls.stderr.on("data", { data -> stderr.add("$data".take(80));while (stderr.size > 5) stderr.removeFirst() })
        ls.on("close") { c ->
            if (r++ == 0) cont.resume(SpawnResult(c, stdout.joinToString(), stderr.joinToString()))
        }
        ls.on("error") { err ->
            if (r++ == 0) cont.resumeWithException(Exception("Error: spawn($cmdLine):${err}"))
        }
    }.onFailure { it.printStackTrace() }
}
