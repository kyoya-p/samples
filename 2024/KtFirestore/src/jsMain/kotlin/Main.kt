import dev.gitlive.firebase.*
import dev.gitlive.firebase.externals.initializeApp
import dev.gitlive.firebase.firestore.firestore

val options = FirebaseOptions(
    apiKey = "AIzaSyCiiIwgR3-hqUrIeCCdmudOr2nKwmviSyU",
    projectId = "road-to-iot",
    databaseUrl = "https://road-to-iot.firebaseio.com",
    applicationId = "1:307495712434:web:e826b0016881e1b5f33bab",
)

val firebase = initializeApp(options)

suspend fun main() {



    val db = Firebase.firestore
    db.document("xxx").set("{xxx:xxx}")


    println("Hello, World!")
}
