import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.firestore.*
import dev.gitlive.firebase.initialize
import kotlin.coroutines.resume
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

data class SpawnResult(val exitCode: Int, val stdout: String, val stderr: String)

suspend fun spawn(cmdLine: String) = suspendCoroutine { cont ->
    val child_process = require("child_process")
    val (cmd, args) = cmdLine.split(" ").let { it.first() to it.drop(1).toTypedArray() }
    val ls = child_process.spawn(cmd, args)
    val stdout = mutableListOf<String>()
    val stderr = mutableListOf<String>()
    ls.stdout.on("data") { data -> stdout.add("$data") }
    ls.stderr.on("data", { data -> stderr.add("$data") })
    ls.on("close") { code -> cont.resume(SpawnResult(code, stdout.joinToString(), stderr.joinToString())) }
}

data class Request(
    val isComplete: Boolean,
    val cmd: String,
    val time: Timestamp,
    val result: SpawnResult?,
)

suspend fun DocumentReference.getRequest() = get().data<Map<String, Any?>>().let {
    Request(
        isComplete = it["isComplete"] as Boolean,
        cmd = it["cmd"] as String,
        time = it["time"] as Timestamp,
        result = it["result"] as SpawnResult?,
    )
}

suspend fun DocumentReference.setRequest(r: Request) = update(
    "isComplete" to r.isComplete,
    "cmd" to r.cmd,
    "time" to r.time,
    "result" to r.result,
)

val sampleRequest = Request(isComplete = false, cmd = "ls -l", time = Timestamp.now(), result = null)

suspend fun main() = runCatching {
    val args = (process.argv as Array<String>).drop(2)
    val (tg, pw) = args
    val tgRef = db.collection("fireshell").document(tg)

    tgRef.collection("requests").document("a").setRequest(sampleRequest) // test data

    tgRef.collection("requests").orderBy("time", Direction.ASCENDING).where { "isComplete" notEqualTo true }
        .limit(10).snapshots.collect {
            println("X2")
            it.documents.forEach { ds ->
                val req = ds.reference.getRequest()
                val res = spawn(req.cmd)
                println(res)
                ds.reference.setRequest(req.copy(result = res, isComplete = true))
            }
        }
}.onFailure { println(it.stackTraceToString()) }.getOrElse { }
