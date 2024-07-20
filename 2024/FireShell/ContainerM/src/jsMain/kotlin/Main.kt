import dev.gitlive.firebase.*
import dev.gitlive.firebase.firestore.*
import dev.gitlive.firebase.firestore.Timestamp.Companion.now
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
    val cookies = document.cookie.split(";").filter { it.trim().isNotEmpty() }.map { it.trim().split("=", limit = 2) }
        .associate { it[0] to (it.getOrElse(1) { "" }) }

    val targetId = queryParameters(window.location.search).getOrElse("tg") { "default" }
    val refTg = db.collection("fireshell").document(targetId)
    val ctr = Ctr(refTg)

    val body = document.body ?: error("body is null")

    fun field(name: String, default: String, opt: HTMLInputElement.() -> Unit = {}, act: (String) -> Unit = {}) =
        document.create.input(name = name).apply {
            defaultValue = cookies[name] ?: default
            onchange = { document.cookie = "$name=$value";act(value) }
            opt()
        }

    val imageId = field("newImageId", "")
    val loginId = field("loginId", "")
    val cred = field("cred", "", opt = { type = "password" })
    body.append { div { +"Target: $targetId" }.onclick = { } }
    body.appendChild(imageId)
    body.appendChild(loginId)

    body.appendChild(cred)
    body.append { button { +"PULL" }.onclick = { ctr.pullImage(imageId.value) } }

    val tableReqs = document.create.table().apply { addClass("table") }
    body.appendChild(tableReqs)
    GlobalScope.launch {
        refTg.collection("requests").orderBy("time", Direction.DESCENDING).limit(25).snapshots.collect { qs ->
            with(tableReqs) {
                innerHTML = ""
                tHead =
                    document.create.thead { tr { td { +"COMMAND" };td { +"CODE" };td { +"OUTPUT" };td { +"EXCEPTION" } } }
                qs.documents.forEachIndexed { i, it ->
                    if (i < 3) {
                        val req = it.data<Request>()
                        insertRow().apply {
                            insertCell().textContent = req.cmd
                            insertCell().textContent = "${req.result?.exitCode}"
                            insertCell().textContent = req.result?.run { stdout.split("\n").last() }
                            insertCell().textContent = req.result?.stderr
                        }
                    } else {
                        it.reference.delete()
                    }
                }
            }
        }
    }
    val book = document.create.table().apply { addClass("table") }
    body.appendChild(book)

    with(book) {
        innerHTML = ""
        tHead = document.create.thead { tr { td { +"IMAGE ID" } } }
        ctr.listImage {
            println("[$it]")
            document.create.tr {
                document.create.td {
                    div {
                        +it
                        button {}
                    }
                }
            }
        }
    }
//    ctr.imagesSnapshots.collect { qs ->
//        with(book) {
//            innerHTML = ""
//            tHead = document.create.thead {
//                tr { td { +"IMAGE ID"; button { +"UPDATE"; onclick = { ctr.listImage() } } } }
//            }
//            qs.documents.forEach { ds ->
//                println(ds.get<String>("name"))
//                document.create.tr { document.create.td { div { +ds.get<String>("name"); button {} } } }
//            }
//        }
//    }
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
        ctr("i pull ${if (cred.isNullOrEmpty()) "" else "-u $cred "}$id", op)

    fun listImage(op: (String) -> Unit) = ctr("i ls -q") {
        val refImgs = refTarget.collection("images")
//        val f = refImgs.snapshots.first().documents.forEach { it.reference.delete() }
        println(it.stdout)
        it.stdout.split("\n").filter { it.isNotEmpty() }.forEach {
//            refImgs.document(it.replace("/", "-")).set(mapOf("name" to it, "time" to now()))
            op(it)
        }
    }

    val imagesSnapshots = refTarget.collection("images").snapshots
}
