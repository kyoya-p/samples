import dev.gitlive.firebase.*
import dev.gitlive.firebase.firestore.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.dom.addClass
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.*
import kotlinx.html.org.w3c.dom.events.Event


@OptIn(DelicateCoroutinesApi::class)
suspend fun rpcViewer() {
    val ckTargetId = Cookie("targetId", "default")

    val urlTargetId = queryParameters(window.location.search).getOrElse("tg") { ckTargetId.value }
    val refTg = db.collection("fireshell").document(urlTargetId)

    val body = document.body ?: error("body is null")
    fun <T> TagConsumer<T>.act(label: String, op: (Event) -> Unit) = button { +label; onClickFunction = op }

    body.append { p { +"Target: $urlTargetId" } }
    val tableReqs = document.create.table().apply { addClass("table") }
    body.appendChild(tableReqs)
    GlobalScope.launch {
        refTg.collection("requests").orderBy("time", Direction.DESCENDING).limit(25).snapshots.collect { qs ->
            with(tableReqs) {
                innerHTML = ""
                tHead =
                    document.create.thead { tr { td { +"TIME" };td { +"COMMAND" };td { +"CODE" };td { +"OUTPUT" };td { +"EXCEPTION" } } }
                qs.documents.forEachIndexed { i, it ->
                    val req = it.data<Request>()
                    insertRow().apply {
                        val t= Instant.fromEpochSeconds(req.time.seconds).toLocalDateTime(TimeZone.currentSystemDefault())
                        insertCell().textContent = "$t"
                        insertCell().textContent = req.cmd
                        insertCell().textContent = "${req.result?.exitCode}"
                        insertCell().textContent = req.result?.stdout
                        insertCell().textContent = req.result?.stderr
                    }
                }
            }
        }
    }

}
