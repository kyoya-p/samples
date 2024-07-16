import dev.gitlive.firebase.*
import dev.gitlive.firebase.firestore.*
import kotlinx.browser.document
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.dom.addClass
import kotlinx.html.dom.create
import kotlinx.html.js.input
import kotlinx.html.js.table
import kotlinx.html.js.button


val options = FirebaseOptions(
    apiKey = appKey,
    projectId = "road-to-iot",
    databaseUrl = "https://road-to-iot.firebaseio.com",
    applicationId = "1:307495712434:web:638b142c284daabef33bab",
)
val app = Firebase.initialize(Unit, options)
val db = Firebase.firestore(app).apply {
    settings = firestoreSettings(settings) { cacheSettings = persistentCacheSettings { } }
}

@OptIn(DelicateCoroutinesApi::class)
suspend fun main() {
    val ctr = Ctr(db.collection("fireshell").document("default"))

    val body = document.body ?: error("body is null")
    val book = document.create.table().apply { addClass("table") }

    val action = document.create.input().apply { onkeyup = { if (it.key == "Enter") ctr.pullImage(value) {} } }

    body.append(action)
    body.append(book)

//    document.create.input(name = "name").apply { onkeyup = { if (it.key == "Enter") addItem(value) }
    ctr.updateImageInfo()
    ctr.imagesSnapshots.collect { qs ->
        book.innerHTML = ""
        qs.documents.forEach { ds ->
            book.insertRow().apply {
                insertCell().textContent = ds.get<String>("name")
                insertCell().append { button { +"DEL" }}
            }
        }
    }

//    refTgReqs.orderBy("time", Direction.DESCENDING).snapshots.collect { qs ->
//        book.innerHTML = ""
//        book.tHead =
//            document.create.thead { tr { td {};td { +"COMMAND" };td { +"OUTPUT" };td { +"CODE" };td { +"EXCEPTION" } } }
//        book.insertRow().apply {
//            insertCell().append { button { +"ENTER" }.onclick = { addItem(action.value) } }
//            insertCell().append(action)
//            insertCell()
//            insertCell()
//            insertCell()
//        }
//        qs.documents.forEach { ds ->
//            val req = ds.data<Request>()
//            book.insertRow().apply {
//                insertCell().append { button { +"DEL" }.onclick = { delItem(ds.id) } }
//                insertCell().apply { textContent = req.cmd }
//                insertCell().apply { textContent = req.result?.stdout }
//                insertCell().apply { textContent = "${req.result?.exitCode}" }
//                insertCell().apply { textContent = req.result?.stderr }
//            }
//        }
//    }
}

suspend fun CollectionReference.addItem(cmd: String) = add(Request(cmd))

class Ctr(val refTarget: DocumentReference) {
    @OptIn(DelicateCoroutinesApi::class)
    fun ctr(cmd: String, op: suspend (SpawnResult) -> Unit) = GlobalScope.async {
        val r = refTarget.collection("requests").add(Request("ctr $cmd")).snapshots()
            .map { it.data<Request>() }.filter { it.isComplete }.map { it.result }.filterNotNull().first()
        op(r)
    }

    fun pullImage(
        image: String, tag: String, registory: String = "docker.io", namespace: String = "library",
        cred: String? = null, op: (SpawnResult) -> Unit
    ) = pullImage("$registory/$namespace/$image:$tag", cred, op)

    fun pullImage(id: String, cred: String? = null, op: (SpawnResult) -> Unit) =
        ctr("i pull ${cred ?: "-u $cred"} $id") {

        }

    fun updateImageInfo() = ctr("i ls -q") {
        val refImgs = refTarget.collection("images")
        val f = refImgs.snapshots.first().documents.forEach { it.reference.delete() }
        it.stdout.split("\n").filter { it.isNotEmpty() }.forEach {
//            refImgs.add(mapOf("name" to it, "time" to now()))
        }
    }

    val imagesSnapshots = refTarget.collection("images").snapshots
}
