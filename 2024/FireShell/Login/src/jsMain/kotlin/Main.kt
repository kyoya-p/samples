import dev.gitlive.firebase.*
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.firestoreSettings
import dev.gitlive.firebase.firestore.persistentCacheSettings
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.dom.clear
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.*
import kotlinx.serialization.Serializable
import org.w3c.dom.HTMLInputElement

val options = FirebaseOptions(
    apiKey = "AIzaSyBg5ssUSPQlEKxZ6zoBrg-hwhoMzwWLQPQ",
    projectId = "riot-7a79a",
    applicationId = "1:749774078339:web:9d60dff9671ab8e9ad76b6",
)
val app = Firebase.initialize(Unit, options)

val db = Firebase.firestore(app)
    .apply { settings = firestoreSettings(settings) { cacheSettings = persistentCacheSettings { } } }
val auth = Firebase.auth(app)

suspend fun main() = Firebase.auth(app).authStateChanged.collect { user ->
    when (user) {
        null -> loginPage()
        else -> appPage(user)
    }
}

@OptIn(DelicateCoroutinesApi::class)
suspend fun loginPage() = document.body!!.apply { clear() }.append {
    var userId = ""
    var password = ""
    fun login() = MainScope().launch {
        runCatching { auth.signInWithEmailAndPassword(userId, password) }.onFailure { window.alert("Failed.") }
    }
    p { +"USER ID:"; input_x { userId = it } }
    p { +"PASSWORD:"; input_x { password = it } }
    p { button { +"LOGIN"; onClickFunction = { login() } } }
}

@Serializable
data class Status(val uid: String, val email: String, val status: String, val time: Instant = now())

val refAppRoot = db.collection("fireshell")

fun <T> TagConsumer<T>.input_x(opt: suspend INPUT.() -> Unit = {}, chg: suspend (v: String) -> Unit) = input {
    MainScope().launch { opt() }
    onChangeFunction = { MainScope().launch { chg((it.target as HTMLInputElement).value) } }
}

@OptIn(DelicateCoroutinesApi::class)
suspend fun appPage(user: FirebaseUser) = document.body!!.apply { clear() }.append {
    document.body!!.apply { clear() }.append {
        p { button { +"LOGOUT "; onClickFunction = { MainScope().launch { auth.signOut() } } } }
        p {
            +"MY STATUS:"
            val refDoc = refAppRoot.document(user.uid)
            input_x({ value = refDoc.get().data<Status>().status }) { v ->
                refDoc.set(Status(user.uid, user.email ?: "UNK", v))
            }
        }

        refAppRoot.orderBy("time", Direction.DESCENDING).snapshots.collect {
            table {
                classes = setOf("table")
                qs.documents.filter { it.exists }.map { it.data<Status>() }.forEach { s ->
                    tr {
                        td { +s.time.toLocalDateTime(TimeZone.currentSystemDefault()).toString() }
                        td { +s.email }
                        td { +s.status }
                    }
                }
            }
        }
    }
