import dev.gitlive.firebase.*
import dev.gitlive.firebase.auth.auth
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.dom.addClass
import kotlinx.dom.clear
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.*

suspend fun main() {
    Firebase.auth(app).authStateChanged.collect { user ->
        if (user == null) {
            login()
        } else {
            val queries = queryParameters(window.location.search)
            when (queries["mode"]) {
                "rpc" -> rpcViewer()
                else -> ctrMain()
            }
        }
    }
}

suspend fun login() = document.body!!.apply { clear() }.append {
    val userId = Cookie("uid", "")
    val password = Cookie("pw", "")
    p { +"USER ID"; field(userId) }
    p { +"PASSWORD"; field(password, { type = InputType.password }) }
    p { btn("LOGIN") { auth.signInWithEmailAndPassword(userId.value, password.value) } }
}

@OptIn(DelicateCoroutinesApi::class)
suspend fun ctrMain() =with(document.body!!) {
    val refTg = db.collection("fireshell").document(auth.currentUser!!.uid)
    val ctr = Ctr(refTg)
    val imageId = Cookie("newImageId", "")
    val pullOpts = Cookie("pullOpts", "")
    clear()
    append {
        p { btn("LOGOUT") { GlobalScope.launch { auth.signOut() } } }
        p { +"Target: ${auth.currentUser?.email}" }
        p {
            +"ctr i pull "; field(pullOpts); field(imageId);
            btn("PULL") { ctr.pullImage(imageId.value, pullOpts.value) { ctr.getStatus() } }
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
                    td { btn("DEL") { ctr.deleteImage(img.imageName) { ctr.getStatus() } } }
                    td { +img.imageName }
                    td {
                        +"ctr run "; field(runOpts); +"${img.imageName} ";field(runTask);
                        btn("RUN") {
                            ctr.runContainer(runOpts.value, img.imageName, runTask.value) { ctr.getStatus() }
                        }
                    }
                }
                tbody { imgs.forEach { img -> raw(img) } }
            }
        }
    }
    append(images)

    val procs = document.create.table().apply { addClass("table") }
    GlobalScope.launch {
        val tasks = refTg.collection("tasks").get().documents.map { it.data<Task>() }.associate { it.id to it }
        fun <T> TagConsumer<T>.containerRow(c: Container) = tr {
            td {
                btn("DEL") { ctr.rmContainer(c.id) { ctr.getStatus() } }
                +c.id
            }
            td { +c.imageName }
            td {
                when (val t = tasks[c.id]) {
                    null -> +"-"
                    else -> {
                        +t.status
                        if (t.status == "RUNNING") btn("STOP") { ctr.killTask(c.id) { ctr.getStatus() } }
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
    append(procs)

    ctr.getStatus()
}
