import dev.gitlive.firebase.firestore.*
import kotlinx.browser.document
import kotlinx.coroutines.*
import kotlinx.dom.addClass
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.*
import kotlinx.html.org.w3c.dom.events.Event


@OptIn(DelicateCoroutinesApi::class)
suspend fun rpcViewer() {
    val refUser = refFireShellAppRoot.document(auth.currentUser!!.uid)

    val body = document.body ?: error("body is null")
    fun <T> TagConsumer<T>.act(label: String, op: (Event) -> Unit) = button { +label; onClickFunction = op }
    val ctr = Ctr(refUser)
    body.append {
        val cmd = Cookie("cmd", "")
        p { act("LOGOUT") { MainScope().launch { auth.signOut() } } }
        p { +"Target: ${auth.currentUser?.email}" }
        p { +"RUN"; field(cmd) { MainScope().launch { ctr.rpc(it) } } }

    }
    val tableReqs = document.create.table().apply { addClass("table") }
    body.appendChild(tableReqs)
    MainScope().launch {
        refUser.collection("requests").orderBy("time", Direction.DESCENDING).limit(25).snapshots.collect { qs ->
            with(tableReqs) {
                innerHTML = ""
                tHead = document.create.thead {
                    tr { td { +"TIME" };td { +"COMMAND" };td { +"CMPL" };td { +"CODE" };td { +"OUTPUT" };td { +"EXCEPTION" } }
                }
                qs.documents.forEachIndexed { i, it ->
                    val req = it.data<Request>()
                    insertRow().apply {
                        insertCell().textContent = "${req.time.toInstant()}"
                        insertCell().textContent = req.cmd
                        insertCell().textContent = "${req.isComplete}"
                        insertCell().textContent = "${req.result?.exitCode}"
                        insertCell().textContent = req.result?.stdout
                        insertCell().textContent = req.result?.stderr
                    }
                }
            }
        }
    }
}
