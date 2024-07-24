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

    val tableReqs = document.create.table().apply { addClass("table") }
    body.appendChild(tableReqs)
    GlobalScope.launch {
        refTg.collection("requests").orderBy("time", Direction.DESCENDING).limit(25).snapshots.collect { qs ->
            with(tableReqs) {
                innerHTML = ""
                tHead =
                    document.create.thead { tr { td { +"COMMAND" };td { +"CODE" };td { +"OUTPUT" };td { +"EXCEPTION" } } }
                qs.documents.forEachIndexed { i, it ->
                    if (i < 4) {
                        val req = it.data<Request>()
                        insertRow().apply {
                            insertCell().textContent = req.cmd
                            insertCell().textContent = "${req.result?.exitCode}"
                            insertCell().textContent = req.result?.stdout
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
    GlobalScope.launch {
        ctr.updateImage().collect { imgs ->
            images.innerHTML = ""
            images.append {
                thead { th { td { +"IMAGE" } } }
                fun raw(img: Image) = tr {
                    val runOpts = Cookie("${img.imageName}.opts", "")
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
    body.append(procs)

    ctr.getStatus()
}
