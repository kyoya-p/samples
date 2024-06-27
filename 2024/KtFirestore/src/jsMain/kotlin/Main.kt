import dev.gitlive.firebase.*
import dev.gitlive.firebase.firestore.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.button
import kotlinx.html.js.input
import kotlinx.html.js.table
import kotlinx.serialization.Serializable


val options = FirebaseOptions(
    apiKey = "AIzaSyCiiIwgR3-hqUrIeCCdmudOr2nKwmviSyU\n",
    projectId = "road-to-iot",
    databaseUrl = "https://road-to-iot.firebaseio.com",
    applicationId = "1:307495712434:web:e826b0016881e1b5f33bab",
)

lateinit var firestore: FirebaseFirestore


@OptIn(DelicateCoroutinesApi::class)
suspend fun main() {
    val app = Firebase.apps(Unit).firstOrNull() ?: Firebase.initialize(Unit, options)
    val db = Firebase.firestore(app).apply {
        settings = firestoreSettings(settings) {
            cacheSettings = memoryCacheSettings {}
//        cacheSettings = persistentCacheSettings {}
        }
    }
    val name = document.create.input(name = "name")
    val mail = document.create.input(name = "mail")
    val book = document.create.table()

    println("Start!!")
    val body = document.body ?: error("body is null")

    body.append {
        div { +" Name:" }.append(name)
        div { +" Mail:" }.append(mail)
        button { +"Add" }.onclick = {
            GlobalScope.launch {
                db.collection("tmp").add(mapOf("name" to name.value, "mail" to mail.value))
            }
        }
        div {}.append(book)
    }


    db.collection("tmp").snapshots.collect {
        it.documents.forEachIndexed { i, ds ->
            book.insertRow().apply {
                insertCell().append {
                    button { +"Del" }.onclick = { GlobalScope.launch { db.document(ds.id).delete() } }
                }
                insertCell().apply { textContent = ds.id }
                insertCell().apply { textContent = ds.get<String>("name") }
                insertCell().apply { textContent = ds.get<String>("mail") }
            }
        }
    }
    val timeP = document.create.p { +"Time: 0" }
    body.append(timeP)
    var time = 0
    window.setInterval({
        time++
        timeP.textContent = "Time: $time"
        return@setInterval null
    }, 1000)
}

fun initializeFirebase(persistenceEnabled: Boolean = false) {

    val app = Firebase.apps(Unit).firstOrNull() ?: Firebase.initialize(Unit, options)

    firestore = Firebase.firestore(app).apply {
//        useEmulator(emulatorHost, 8080)
        settings = firestoreSettings(settings) {
            cacheSettings = if (persistenceEnabled) {
                persistentCacheSettings { }
            } else {
                memoryCacheSettings { }
            }
        }
    }
}
