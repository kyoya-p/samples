import dev.gitlive.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class Ctr(val refTarget: DocumentReference) {
    @OptIn(DelicateCoroutinesApi::class)
    fun rpc(cmd: String, op: suspend (SpawnResult) -> Unit) = GlobalScope.launch { op(rpc(cmd)) }
    suspend fun rpc(cmd: String) = refTarget.collection("requests").add(Request(cmd))
        .snapshots().map { it.data<Request>() }.filter { it.isComplete }.map { it.result }.filterNotNull().first()

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
    fun deleteImage(id: String, op: () -> Unit = {}) = ctr("i rm $id") { op() }
    fun updateContainer() =
        refTarget.collection("containers").snapshots.map { it.documents.map { it.data<Container> { } } }

    fun runContainer(opts: String, image: String, taskId: String, op: () -> Unit = {}) =
        ctr("run $opts $image $taskId") { op() }

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

        val imgs = rpc("ctr i ls").stdout.split("\n").drop(1).map { it.split(" ") }.map { Image(it[0]) }
        imgs.updateList(
            collection = refTarget.collection("images"),
            eq = { a, b -> a.imageName == b.imageName },
            makeId = { it.imageName.replace("/", "-") }
        )

        val cs = rpc("ctr c ls").stdout.split("\n").drop(1).filter { it.isNotEmpty() }
            .map { it.split(" ", "\t").filter { it.isNotEmpty() } }.map { Container(id = it[0], imageName = it[1]) }
        cs.updateList(
            collection = refTarget.collection("containers"),
            eq = { a, b -> a.id == b.id },
            makeId = { it.imageName.replace("/", "-") }
        )
    }
}