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
    val app = Firebase.initialize(Unit, options)
    val db = Firebase.firestore(app)
    db.settings = firestoreSettings(db.settings) { cacheSettings = persistentCacheSettings { } }

    fun addItem(item: String) = GlobalScope.launch { db.collection("tmp").add(mapOf("action" to item)) }
    fun delItem(id: String) = GlobalScope.launch { db.collection("tmp").document(id).delete() }

    val action = document.create.input(name = "name")
    val book = document.create.table().apply { addClass("table") }
    val body = document.body ?: error("body is null")
    body.append(book)
    db.collection("fireshell").document("default")
        .collection("requests").orderBy("time",Direction.ASCENDING)
        .snapshots.collect { qs ->
            book.innerHTML = ""
            book.tHead = document.create.thead { tr { td {};td { +"To do List" } } }
            book.insertRow().apply {
                insertCell().append { button { +"Add" }.onclick = { addItem(action.value) } }
                insertCell().append(action)
            }
            qs.documents.forEach { ds ->
                book.insertRow().apply {
                    insertCell().append { button { +"Del" }.onclick = { delItem(ds.id) } }
                    insertCell().apply { textContent = ds.get<String?>("cmd") }
                }
            }
        }
}
