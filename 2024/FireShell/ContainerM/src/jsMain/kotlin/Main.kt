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
import org.w3c.dom.HTMLInputElement

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
    val queryParameters = window.location.search.replaceFirst("?", "").split("&")
        .map { it.split("=") }.associate { it[0] to (it.getOrElse(1) { "" }) }
    val cookies = document.cookie.split(";").filter { it.trim().isNotEmpty() }
        .map { it.trim().split("=", limit = 2) }.associate { it[0] to (it.getOrElse(1) { "" }) }

    val targetId = queryParameters.getOrElse("tg") { "default" }
    fun ctr(targetId: String) = Ctr(db.collection("fireshell").document(targetId))

    val body = document.body ?: error("body is null")
    val book = document.create.table().apply { addClass("table") }

    fun field(name: String, default: String, op: HTMLInputElement.() -> Unit = {}) =
        document.create.input(name = name).apply {
            defaultValue = cookies[name] ?: default
            onchange = { document.cookie = "$name=$value";Unit }
            op()
        }

    val imageId = field("newImageId", "")
    val loginId = field("loginId", "")
    val cred = field("cred", "") { type = "password" }

    body.append { p { +"Target: $targetId" } }
    body.appendChild(imageId)
    body.appendChild(loginId)
    body.appendChild(cred)
    body.append { button { +"PULL" }.onclick = { ctr(targetId).pullImage(imageId.value) } }
    body.appendChild(book)

    ctr(targetId).imagesSnapshots.collect { qs ->
        with(book) {
            innerHTML = ""
            tHead = document.create.thead { tr { td { +"IMAGE ID" } } }
            qs.documents.forEach { ds ->
                insertRow().apply {
                    insertCell().textContent = ds.get<String>("name")
                }
            }
        }
    }
}

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
