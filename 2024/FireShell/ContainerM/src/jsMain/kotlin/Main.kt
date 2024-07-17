import dev.gitlive.firebase.*
import dev.gitlive.firebase.firestore.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.dom.addClass
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.*


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

suspend fun main() {
    val cookies = document.cookie.split(";").map { it.trim().split("=", limit = 2) }.associate { it[0] to it[1] }
    fun ctr(targetId: String) = Ctr(db.collection("fireshell").document(targetId))

    val body = document.body ?: error("body is null")
    val book = document.create.table().apply { addClass("table") }

    fun field(name: String, default: String, op: (String) -> Unit) = document.create.input(name = name).apply {
        defaultValue = cookies[name] ?: default
        onchange = { document.cookie = "$name=$value"; op(value) }
    }

    val targetId = field("targetId", "default") {}
    val imageId = field("newImageId", "") {}

    body.appendChild(targetId)
    body.appendChild(imageId)
    body.append {
        form {
            id = "form1"
            onSubmitFunction = {}
        }
        button { +"PULL" }.onclick = { ctr(targetId.value).pullImage(imageId.value) }
    }
    body.appendChild(book)

//    document.create.input(name = "name").apply { onkeyup = { if (it.key == "Enter") addItem(value) }
    ctr(targetId.value).updateImageInfo()
    ctr(targetId.value).imagesSnapshots.collect { qs ->
        book.innerHTML = ""
        qs.documents.forEach { ds ->
            book.insertRow().apply {
                insertCell().textContent = ds.get<String>("name")
//                insertCell().append { button { +"DEL" }}
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
        val r = refTarget.collection("requests").add(Request("ctr $cmd")).snapshots().map { it.data<Request>() }
            .filter { it.isComplete }.map { it.result }.filterNotNull().first()
        op(r)
    }

    fun pullImage(
        image: String,
        tag: String,
        registory: String = "docker.io",
        namespace: String = "library",
        cred: String? = null,
        op: (SpawnResult) -> Unit = {}
    ) = pullImage("$registory/$namespace/$image:$tag", cred, op)

    fun pullImage(id: String, cred: String? = null, op: (SpawnResult) -> Unit = {}) =
        ctr("i pull ${if (cred != null && cred != "") "-u $cred" else ""} $id", op)

    fun updateImageInfo() = ctr("i ls -q") {
        val refImgs = refTarget.collection("images")
        val f = refImgs.snapshots.first().documents.forEach { it.reference.delete() }
        it.stdout.split("\n").filter { it.isNotEmpty() }.forEach {
//            refImgs.add(mapOf("name" to it, "time" to now()))
        }
    }

    val imagesSnapshots = refTarget.collection("images").snapshots
}
