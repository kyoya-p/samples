import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.firestoreSettings
import dev.gitlive.firebase.firestore.memoryCacheSettings
import dev.gitlive.firebase.initialize
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

val options = FirebaseOptions(
    apiKey = "AIzaSyBg5ssUSPQlEKxZ6zoBrg-hwhoMzwWLQPQ",
    projectId = "riot-7a79a",
    applicationId = "1:749774078339:web:9d60dff9671ab8e9ad76b6",
)
val app = Firebase.initialize(Unit, options)
val auth = Firebase.auth(app)
val db = Firebase.firestore(app).apply {
//    settings = firestoreSettings(settings) { cacheSettings = persistentCacheSettings { } }
    settings = firestoreSettings(settings) { cacheSettings = memoryCacheSettings { } }
}
val refFireShellAppRoot = db.collection("fireshell")

fun Instant.toTimestamp() = Timestamp(epochSeconds, toLocalDateTime(TimeZone.currentSystemDefault()).nanosecond)
fun Timestamp.toInstant() = Instant.fromEpochMilliseconds(seconds * 1000L + nanoseconds / 1000000L)

