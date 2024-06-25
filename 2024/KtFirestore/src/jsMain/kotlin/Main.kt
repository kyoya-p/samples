import dev.gitlive.firebase.*
import dev.gitlive.firebase.firestore.*


val options = FirebaseOptions(
    apiKey = "AIzaSyCiiIwgR3-hqUrIeCCdmudOr2nKwmviSyU",
    projectId = "road-to-iot",
    databaseUrl = "https://road-to-iot.firebaseio.com",
    applicationId = "1:307495712434:web:e826b0016881e1b5f33bab",
)

lateinit var firestore: FirebaseFirestore

suspend fun main() {
    initializeFirebase()
    firestore.document("tmp/xxx").set(mapOf("aaa" to "bbb"))
    println("Hello, World!")
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
