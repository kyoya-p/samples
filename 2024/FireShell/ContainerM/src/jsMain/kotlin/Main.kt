import dev.gitlive.firebase.*
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.dom.addClass
import kotlinx.dom.clear
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.*

//val options = FirebaseOptions(
//    apiKey = appKey,
//    projectId = "road-to-iot",
//    databaseUrl = "https://road-to-iot.firebaseio.com",
//    applicationId = "1:307495712434:web:98565c9f7af0beb3f33bab",
//)
//val app = Firebase.initialize(Unit, options)
//val db = Firebase.firestore(app).apply {
//    settings = firestoreSettings(settings) { cacheSettings = persistentCacheSettings { } }
//}
//val auth = Firebase.auth(app)

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
    p {
        btn("LOGIN") {
//            window.alert("${userId.value}, ${password.value}")
            val c = auth.signInWithEmailAndPassword(userId.value, password.value)
//            window.alert("${c.user?.email}/")
        }
    }
}


@OptIn(DelicateCoroutinesApi::class)
suspend fun ctrMain() {
//    val ckTargetId = Cookie("targetId", "default")
//    val urlTargetId = queryParameters(window.location.search).getOrElse("tg") { ckTargetId.value }
    val refTg = db.collection("fireshell").document(auth.currentUser!!.uid)
    val ctr = Ctr(refTg)

    val body = document.body ?: error("body is null")

    val imageId = Cookie("newImageId", "")
    val pullOpts = Cookie("pullOpts", "")

    body.clear()
    body.append {
        fun signout() = GlobalScope.launch { auth.signOut() }
        p { btn("LOGOUT") { signout() } }
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
    body.append(images)

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
    body.append(procs)

    ctr.getStatus()
}
