import dev.gitlive.firebase.*
import dev.gitlive.firebase.firestore.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.p


val options = FirebaseOptions(
    apiKey = "xxxxxxx-xxxxxxxx",
    projectId = "road-to-iot",
    databaseUrl = "https://road-to-iot.firebaseio.com",
    applicationId = "1:307495712434:web:e826b0016881e1b5f33bab",
)

lateinit var firestore: FirebaseFirestore

suspend fun main() {
    initializeFirebase()
    firestore.document("tmp/12345").set(mapOf("addrbook" to listOf("aaa@bbb.jp")))
    println("Hello, World!")

    val body = document.body ?: error("No body")
    body.append {
        div {
            p {
                +"Here is "
                a("https://kotlinlang.org") { +"official Kotlin site" }
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

    val app = Firebase.apps(Unit).firstOrNull() ?: Firebase.initialize(
        Unit, options
//        FirebaseOptions(
//            applicationId = "1:846484016111:ios:dd1f6688bad7af768c841a",
//            apiKey = "AIzaSyCK87dcMFhzCz_kJVs2cT2AVlqOTLuyWV0",
//            databaseUrl = "https://fir-kotlin-sdk.firebaseio.com",
//            storageBucket = "fir-kotlin-sdk.appspot.com",
//            projectId = "fir-kotlin-sdk",
//            gcmSenderId = "846484016111"
//        )
    )

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
