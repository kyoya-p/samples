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
import kotlinx.html.org.w3c.dom.events.Event
import org.w3c.dom.Element
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

@OptIn(DelicateCoroutinesApi::class)
suspend fun main() {
    val cookies = document.cookie.split(";").filter { it.trim().isNotEmpty() }.map { it.trim().split("=", limit = 2) }
        .associate { it[0] to (it.getOrElse(1) { "" }) }

    val targetId = queryParameters(window.location.search).getOrElse("tg") { "default" }
    val refTg = db.collection("fireshell").document(targetId)
    val ctr = Ctr(refTg)

    val body = document.body ?: error("body is null")

    fun fieldX(name: String, default: String, opt: HTMLInputElement.() -> Unit = {}, act: (String) -> Unit = {}) =
        document.create.input(name = name).apply {
            defaultValue = cookies[name] ?: default
            onchange = { document.cookie = "$name=$value";act(value) }
            opt()
        }

    fun <T> TagConsumer<T>.act(label: String, op: (Event) -> Unit) = button { +label; onClickFunction = op }
    class Field(val value: String, val name: String)

    fun <T> TagConsumer<T>.field(eid: String, default: String, onChange: (Element, Event) -> Unit) = input {
        id = eid; name = eid; value = default
        onChangeFunction = { ev ->
            val i = document.querySelector("input[id='$eid']")!!
            onChange(i, ev)
        }
    }

    val imageId = fieldX("newImageId", "")
    val loginId = fieldX("loginId", "")
    val cred = fieldX("cred", "", opt = { type = "password" })
    body.append { div { +"Target: $targetId" }.onclick = { } }
    body.append(imageId)
    body.append(loginId)
    body.append(cred)
    body.append { act("PULL") { ctr.pullImage(imageId.value) { ctr.listImage() } } }

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

    val images = document.create.table().apply { addClass("table") }
    ctr.listImage()
    GlobalScope.launch {
        ctr.updateImage().collect { imgs ->
            images.innerHTML = ""
            images.append {
                thead { th { td { +"IMAGE" } } }
                fun raw(img: Image) = tr {
                    td { act("DEL") { ctr.deleteImage(img.imageName) { ctr.listImage() } } }
                    td { +img.imageName }
                    td {
                        +"ctr run "
                        field("opts", "") { e, ev ->
                            window.alert("${e.asDynamic().value}")
                        }
                        +"${img.imageName} "
                        field("task:", "") { e, ev -> }
                        act("RUN") { }
                    }
                }
                tbody { imgs.forEach { img -> raw(img) } }
            }
        }
    }
    body.append(images)

    val procs = document.create.table().apply { addClass("table") }
    GlobalScope.launch {
        ctr.updateContainer().collect { imgs ->
            procs.innerHTML = ""
            procs.append {
                thead { th { td { +"ID" };td { +"IMAGE NAME" } } }
                tbody {
                    imgs.forEach { c ->
                        tr {
                            td { act("STOP") { }; +c.id }
                            td { +c.imageName }
                        }
                    }
                }
            }
        }
    }
    ctr.listContainer()
    body.append(procs)
}

class Ctr(val refTarget: DocumentReference) {
    @OptIn(DelicateCoroutinesApi::class)
    fun rpc(cmd: String, op: suspend (SpawnResult) -> Unit) = GlobalScope.launch {
        val r = refTarget.collection("requests").add(Request(cmd)).snapshots().map { it.data<Request>() }
            .filter { it.isComplete }.map { it.result }.filterNotNull().first()
        op(r)
    }

    fun ctr(cmd: String, op: suspend (SpawnResult) -> Unit) = rpc("ctr $cmd", op)

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

    fun listImage(op: (List<String>) -> Unit = {}) = ctr("i ls -q") {
        val r = it.stdout.split("\n").filter { it.isNotEmpty() }
        val refImages = refTarget.collection("images")
        refImages.get().documents.forEach { if (!r.contains(it.data<Image>().imageName)) it.reference.delete() }
        r.forEach { refImages.document(it.replace("/", "-")).set(Image(it)) }
        op(r)
    }

    fun updateImage() = refTarget.collection("images").snapshots.map { it.documents.map { it.data<Image> { } } }

    fun deleteImage(id: String, op: () -> Unit = {}) = ctr("i rm $id") { op() }

    fun listContainer(op: (List<Container>) -> Unit = {}) = ctr("c ls") { res ->
        val r = res.stdout.split("\n").drop(1).filter { it.isNotEmpty() }
            .map { it.split(" ", "\t").filter { it.isNotEmpty() }.let { println(it);Container(it[0], it[1]) } }
        val refContainer = refTarget.collection("containers")
        refContainer.get().documents.filter { r.map { it.id }.contains(it.id) }.forEach { ds ->
            val c = ds.data<Container>()
            if (!r.map { it.id }.contains(c.id)) ds.reference.delete()
        }
        r.forEach { refContainer.document(it.id.replace("/", "-")).set(Container(it.id, it.imageName)) }
        op(r)
    }

    fun updateContainer() =
        refTarget.collection("containers").snapshots.map { it.documents.map { it.data<Container> { } } }

    fun runContainer(image: String, taskId: String) = ctr("run $image $taskId") {}
}
