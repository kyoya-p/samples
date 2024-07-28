import dev.gitlive.firebase.*
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.Timestamp.Companion.now
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.firestoreSettings
import dev.gitlive.firebase.firestore.persistentCacheSettings
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.dom.clear
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.*
import org.w3c.dom.HTMLInputElement
import kotlin.js.Json

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
        else -> applicationPage(user)
    }
}

@OptIn(DelicateCoroutinesApi::class)
suspend fun loginPage() = document.body!!.apply { clear() }.append {
    var userId = ""
    var password = ""
    fun login() = GlobalScope.launch {
        runCatching { auth.signInWithEmailAndPassword(userId, password) }.onFailure { window.alert("Failed.") }
    }
    p { +"USER ID:"; input { onChangeFunction = { userId = (it.target as HTMLInputElement).value } } }
    p { +"PASSWORD:"; input { onChangeFunction = { password = (it.target as HTMLInputElement).value } } }
    p { button { +"LOGIN"; onClickFunction = { login() } } }
}

data class MyStatus(val uid: String, val status: String, val time: Timestamp = now())

@OptIn(DelicateCoroutinesApi::class)
suspend fun applicationPage(user: FirebaseUser) = db.collection("fireshell").orderBy("time").snapshots.collect { qs ->
    document.body!!.apply { clear() }.append {
        p { button { +"LOGOUT"; onClickFunction = { GlobalScope.launch { auth.signOut() } } } }
        println(qs.documents.size)

        qs.documents.filter { it.exists }.forEach { ds ->

                    val docStatus = ds.get<String>("uid")
        //            p {
//                +"STATUS: "
//                input {
//                    value = docStatus.status
//                    onChangeFunction = {
//                        val newStatus = (it.target as HTMLInputElement).value
//                        MainScope().launch { ds.reference.set(docStatus.copy(status = newStatus, time = now())) }
//                    }
//                }
//            }
        }
    }
}

