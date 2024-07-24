import dev.gitlive.firebase.*
import dev.gitlive.firebase.firestore.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.dom.addClass
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.*
import kotlinx.html.org.w3c.dom.events.Event

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
    val queries = queryParameters(window.location.search)
    if (queries["mode"] == "rpc") {
        rpcViewer()
    } else {
        ctrMain()
    }
}

@OptIn(DelicateCoroutinesApi::class)
suspend fun ctrMain() {

    val ckTargetId = Cookie("targetId", "default")

    val urlTargetId = queryParameters(window.location.search).getOrElse("tg") { ckTargetId.value }
    val refTg = db.collection("fireshell").document(urlTargetId)
    val ctr = Ctr(refTg)

    val body = document.body ?: error("body is null")
    fun <T> TagConsumer<T>.act(label: String, op: (Event) -> Unit) = button { +label; onClickFunction = op }

    val imageId = Cookie("newImageId", "")
    val pullOpts = Cookie("pullOpts", "")

    body.append { p { +"Target: $urlTargetId" } }
    body.append {
        p {
            +"ctr i pull "; field(pullOpts); field(imageId);
            act("PULL") { ctr.pullImage(imageId.value, pullOpts.value) { ctr.getStatus() } }
        }
    }

    val images = document.create.table().apply { addClass("table") }
    GlobalScope.launch {
        ctr.updateImage().collect { imgs ->
            images.innerHTML = ""
            images.append {
                thead { th { td { +"IMAGE" } } }
                fun raw(img: Image) = tr {
                    val runOpts = Cookie("${img.imageName}.opts", "-d")
                    val runTask = Cookie("${img.imageName}.task", "")
                    td { act("DEL") { ctr.deleteImage(img.imageName) { ctr.getStatus() } } }
                    td { +img.imageName }
                    td {
                        +"ctr run "; field(runOpts); +"${img.imageName} ";field(runTask);
                        act("RUN") {
                            ctr.runContainer(runOpts.value, img.imageName, runTask.value) { ctr.getStatus() }
                        }
                    }
                }
                tbody { imgs.forEach { img -> raw(img) } }
            }
        }
    }
    body.append(images)

    val procs = document.create.table().apply { addClass("table") }
    GlobalScope.launch {
        val tasks = refTg.collection("tasks").get().documents.map { it.data<Task>() }.associate { it.id to it }
        fun <T> TagConsumer<T>.containerRow(c: Container) = tr {
            td {
                act("DEL") { ctr.rmContainer(c.id) { ctr.getStatus() } }
                +c.id
            }
            td { +c.imageName }
            td {
                when (val t = tasks[c.id]) {
                    null -> +"-"
                    else -> {
                        +t.status
                        if (t.status == "RUNNING") act("STOP") { ctr.killTask(c.id) { ctr.getStatus() } }
                    }
                }
            }
        }

        ctr.updateContainer().collect { conts ->
            procs.innerHTML = ""
            procs.append {
                thead { th { td { +"ID" };td { +"IMAGE NAME" } } }
                tbody {
                    conts.forEach { c -> containerRow(c) }
                }
            }
        }
    }
    body.append(procs)

    ctr.getStatus()
}
