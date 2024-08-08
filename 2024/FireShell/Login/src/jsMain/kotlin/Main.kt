import dev.gitlive.firebase.*
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
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
import kotlinx.html.dom.create
import kotlinx.html.js.*
import kotlinx.serialization.Serializable

val options = FirebaseOptions(
    apiKey = "AIzaSyBg5ssUSPQlEKxZ6zoBrg-hwhoMzwWLQPQ",
    projectId = "riot-7a79a",
    applicationId = "1:749774078339:web:9d60dff9671ab8e9ad76b6",
)
val app = Firebase.initialize(Unit, options)
val auth = Firebase.auth(app)

val db = Firebase.firestore(app).apply {
    settings = firestoreSettings(settings) { cacheSettings = persistentCacheSettings { } }
}

suspend fun main() = Firebase.auth(app).authStateChanged.collect { user ->
    when (user) {
        null -> loginPage()
        else -> appPage(user)
    }
}

//fun <T> TagConsumer<T>.inputx(opt: INPUT.() -> Unit = {}, chg: suspend (v: String) -> Unit = {}) = input {
//    opt()
//    onChangeFunction = { MainScope().launch { chg((it.target as HTMLInputElement).value) } }
//}

//suspend fun loginPage() = document.body!!.apply { clear() }.append {
//    var userId = ""
//    var password = ""
//    fun login() = MainScope().launch {
//        runCatching { auth.signInWithEmailAndPassword(userId, password) }.onFailure { window.alert("Failed.") }
//    }
//    p { +"USER ID:"; inputx { userId = it } }
//    p { +"PASSWORD:"; inputx({ type = InputType.password }) { password = it } }
//    p { button { +"LOGIN"; onClickFunction = { login() } } }
//}

suspend fun appPage(user: FirebaseUser) = document.body!!.apply { clear() }.append {
    @Serializable
    data class User(val uid: String, val email: String, val status: String, val time: Instant = now())

    fun initStatus() = User(user.uid, user.email ?: "", "")
    suspend fun errCk(op: suspend () -> Unit) = runCatching { op() }.onFailure { window.alert("${it.message}") }
    val refAppRoot = db.collection("fireshell")

    val table = document.create.table()
    p { logoutButton(); +"${user.email}" }
    document.body!!.append(table)
    MainScope().launch {
        refAppRoot.document(user.uid).apply { if (!get().exists) set(initStatus()) }
        refAppRoot.snapshots.collect { qs ->
            table.clear()
            table.className = "table"
            table.append {
                qs.documents.filter { it.exists }.forEach { ds ->
                    val st = ds.data<User>()
                    tr {
                        td { +st.email }
                        td { inputx({ value = st.status }) { errCk { ds.reference.set(st.copy(status = it)) } } }
                        td { +st.time.toLocalDateTime(TimeZone.currentSystemDefault()).toString() }
                    }
                }
            }
        }
    }
}
