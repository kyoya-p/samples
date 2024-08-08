import dev.gitlive.firebase.firestore.Direction
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


external val process: dynamic

//val options = FirebaseOptions(
//    apiKey = "AIzaSyBg5ssUSPQlEKxZ6zoBrg-hwhoMzwWLQPQ",
//    projectId = "riot-7a79a",
//    applicationId = "1:749774078339:web:9d60dff9671ab8e9ad76b6",
//)
//
//val app = Firebase.initialize(Unit, options)
//val auth = Firebase.auth(app)
//val db = Firebase.firestore(app)
//    .apply { settings = firestoreSettings(settings) { cacheSettings = memoryCacheSettings { } } }


external fun require(module: String): dynamic

suspend fun main() = runCatching {
    val uid = process.env.USERID as String
    val pw = process.env.PASSWORD as String
    println("UID=$uid")

    val user = auth.signInWithEmailAndPassword(uid, pw).user ?: throw Exception("Failed: signInWithEmailAndPassword()")

    val refUser = refFireShellAppRoot.document(user.uid)
        .apply { if (!get().exists) set(User(uid = user.uid, email = user.email ?: "", status = "")) }
    val refReq = refUser.collection("requests")
    refReq.orderBy("time", Direction.ASCENDING).where { "isComplete" notEqualTo true }.limit(10).snapshots.collect {
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
        ls.stdout.on("data") { data -> stdout.add("$data") }
        ls.stderr.on("data") { data -> stderr.add("$data") }
        ls.on("close") { c -> if (r++ == 0) cont.resume(SpawnResult(c, stdout.joinToString(), stderr.joinToString())) }
        ls.on("error") { err -> if (r++ == 0) cont.resumeWithException(Exception("Error: spawn($cmdLine):${err}")) }
    }.onFailure { it.printStackTrace() }
}
