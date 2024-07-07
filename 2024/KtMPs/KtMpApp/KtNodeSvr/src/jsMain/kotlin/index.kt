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


suspend fun main() = runCatching {
    val args = (process.argv as Array<String>).drop(2)
    val (tg, pw) = args
    val tgRef = db.collection("fireshell").document(tg)
    tgRef.collection("requests").orderBy("time", Direction.ASCENDING).where { "isComplete" notEqualTo true }
        .limit(10).snapshots.collect {
            it.documents.forEach { ds ->
                val cmd = ds.get<String>("cmd")
                val res = spawn(cmd)
                println(res)
                ds.reference.update("isComplete" to true, "result.stdout" to res.stdout, "result.stderr" to res.stderr)
            }
        }
}.onFailure { println(it.stackTraceToString()) }.getOrElse { }
