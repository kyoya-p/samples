import dev.gitlive.firebase.firestore.Direction
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

external val process: dynamic
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
            val res = runCatching {
                print("Run: ${req.cmd} ")
                req.copy(isComplete = true, result = spawn(req.cmd))
            }.getOrElse { req.copy(isComplete = true, exception = it.message) }
            println("-> ${res.result?.exitCode} Ex:${res.exception}")
            ds.reference.set<Request>(res)
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
        ls.on("error") { err -> if (r++ == 0) cont.resumeWithException(Exception("Error: spawn($cmd,$args):${err}")) }
    }.onFailure { cont.resumeWithException(Exception("Error: spawn($cmdLine)")) }
}
