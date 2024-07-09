import dev.gitlive.firebase.*
import dev.gitlive.firebase.firestore.*
import kotlinx.browser.document
import kotlinx.coroutines.*
import kotlinx.dom.addClass
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.button
import kotlinx.html.js.input
import kotlinx.html.js.table
import kotlinx.html.js.thead


val options = FirebaseOptions(
    apiKey = appKey,
    projectId = "road-to-iot",
    databaseUrl = "https://road-to-iot.firebaseio.com",
    applicationId = "1:307495712434:web:638b142c284daabef33bab",
)

@OptIn(DelicateCoroutinesApi::class)
suspend fun main() {
    println("L1")
    val app = Firebase.initialize(Unit, options)
    val db = Firebase.firestore(app)
    db.settings = firestoreSettings(db.settings) { cacheSettings = persistentCacheSettings { } }

    val refTgReqs = db.collection("fireshell").document("default").collection("requests")

    fun addItem(cmd: String) = GlobalScope.launch { refTgReqs.add(Request(cmd)) }
    fun delItem(id: String) = GlobalScope.launch { refTgReqs.document(id).delete() }

    val action = document.create.input(name = "name").apply { onkeyup = { if (it.key == "Enter") addItem(value) } }
    val book = document.create.table().apply { addClass("table") }
    val body = document.body ?: error("body is null")
    body.append(book)
    refTgReqs.orderBy("time", Direction.DESCENDING).snapshots.collect { qs ->
        book.innerHTML = ""
        book.tHead =
            document.create.thead { tr { td {};td { +"COMMAND" };td { +"OUTPUT" };td { +"CODE" };td { +"EXCEPTION" } } }
        book.insertRow().apply {
            insertCell().append { button { +"ENTER" }.onclick = { addItem(action.value) } }
            insertCell().append(action)
            insertCell()
            insertCell()
            insertCell()
        }
        qs.documents.forEach { ds ->
            val req = ds.data<Request>()
            book.insertRow().apply {
                insertCell().append { button { +"DEL" }.onclick = { delItem(ds.id) } }
                insertCell().apply { textContent = req.cmd }
                insertCell().apply { textContent = req.result?.stdout }
                insertCell().apply { textContent = "${req.result?.exitCode}" }
                insertCell().apply { textContent = req.result?.stderr }
            }
        }
    }
}
