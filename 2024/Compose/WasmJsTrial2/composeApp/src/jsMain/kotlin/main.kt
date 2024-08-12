import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.firestoreSettings
import dev.gitlive.firebase.firestore.memoryCacheSettings
import dev.gitlive.firebase.initialize
import kotlinx.browser.document
import kotlinx.browser.window


external fun require(module: String)

val options = FirebaseOptions(
    apiKey = "process.env.APPKEY as String",
    projectId = "xxxxxx",
    databaseUrl = "https://xxxxxxxxxxxxx.firebaseio.com",
    applicationId = "1:xxxxxxxxxxx:web:xxxxxxxxxxxxxxxxxxxx",
)

val app = Firebase.initialize(Unit, options)
val db = Firebase.firestore(app).apply {
    settings = firestoreSettings(settings) { cacheSettings = memoryCacheSettings { } }
}

