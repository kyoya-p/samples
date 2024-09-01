import dev.gitlive.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

class Ctr(val refTarget: DocumentReference) {
    @OptIn(DelicateCoroutinesApi::class)
    fun rpc(cmd: String, op: suspend (SpawnResult) -> Unit) = GlobalScope.launch { op(rpc(cmd)) }
    suspend fun rpc(cmd: String): SpawnResult {
        val refReqs = refTarget.collection("requests")

        val n = now()

        println("$n")
        println("${Instant.fromEpochSeconds(n.toTimestamp().seconds)}")
        println("${n.toTimestamp().toInstant()}")

        refReqs
            .where { "time" lessThan (now() - 20.minutes).toTimestamp() }
            .get().documents.forEach { it.reference.delete() }
        return refReqs.add(Request(cmd)).snapshots().map { it.data<Request>() }
            .filter { it.isComplete }.map { it.result }.filterNotNull().first()
    }

    fun ctr(cmd: String, op: suspend (SpawnResult) -> Unit) = rpc("ctr $cmd", op)

    fun pullImage(
        image: String,
        tag: String,
        registory: String = "docker.io",
        namespace: String = "library",
        cred: String = "",
        op: (SpawnResult) -> Unit = {}
    ) = pullImage("$registory/$namespace/$image:$tag", cred, op)

    fun pullImage(id: String, cred: String, op: (SpawnResult) -> Unit = {}) =
        ctr("i pull ${if (cred.isNotEmpty()) "-u " else ""} $cred $id", op)

    fun updateImage() = refTarget.collection("images").snapshots.map { it.documents.map { it.data<Image> { } } }
    fun deleteImage(id: String, op: (SpawnResult) -> Unit = {}) = ctr("i rm $id", op)
    fun updateContainer() =
        refTarget.collection("containers").snapshots.map { it.documents.map { it.data<Container> { } } }

    fun runContainer(opts: String, image: String, taskId: String, op: () -> Unit = {}) =
        ctr("run $opts $image $taskId") { op() }

    fun rmContainer(taskId: String, op: (SpawnResult) -> Unit = {}) = ctr("c rm $taskId", op)
    fun killTask(taskId: String, op: (SpawnResult) -> Unit = {}) = ctr("tasks kill --signal SIGKILL $taskId", op)
    fun getStatus() = GlobalScope.async {
        suspend inline fun <reified T : Any> List<T>.updateList(
            collection: CollectionReference,
            eq: (a: T, b: T) -> Boolean,
            makeId: (e: T) -> String
        ) {
            collection.get().documents.forEach { ds -> if (!any { eq(it, ds.data<T>()) }) ds.reference.delete() }
            forEach { e ->
                println("[$e]")
                collection.document(makeId(e)).set(e)
            }
        }

        fun String.splitCells() =
            split("\n").drop(1).filter { it.isNotEmpty() }.map { it.split(" ", "\t").filter { it.isNotEmpty() } }

        rpc("ctr task ls").stdout.splitCells().map { Task(id = it[0], pid = it[1], status = it[2]) }.updateList(
            collection = refTarget.collection("tasks"),
            eq = { a, b -> a.id == b.id },
            makeId = { it.id.replace("/", "-") }
        )
        rpc("ctr c ls").stdout.splitCells().map { Container(id = it[0], imageName = it[1]) }.updateList(
            collection = refTarget.collection("containers"),
            eq = { a, b -> a.id == b.id },
            makeId = { it.id.replace("/", "-") }
        )
        rpc("ctr i ls").stdout.splitCells().map { Image(imageName = it[0]) }.updateList(
            collection = refTarget.collection("images"),
            eq = { a, b -> a.imageName == b.imageName },
            makeId = { it.imageName.replace("/", "-") }
        )
    }
}