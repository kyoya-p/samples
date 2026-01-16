import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.initialize
import kotlinx.coroutines.*
import node.api.process
import com.example.model.SampleItem

suspend fun main() {
    js("require('firebase/firestore')")
    val env = process.env
    val options = FirebaseOptions(
        apiKey = env.FB_API_KEY as? String ?: throw IllegalArgumentException("FB_API_KEY is not set."),
//        authDomain = env.FB_AUTH_DOMAIN as? String,
        projectId = env.FB_PROJECT_ID as? String ?: "riot26-70125",
//        storageBucket = env.FB_STORAGE_BUCKET as? String,
        // messagingSenderId = env.FB_MESSAGING_SENDER_ID as? String,
        applicationId = env.FB_APP_ID as? String ?: ""
    )

    Firebase.initialize(context = null, options = options)
    println("Firebase initialized successfully!")

    val db = Firebase.firestore
    println("Firestore initialized.")

    withTimeout(15000) {
        val sampleCollection = db.collection("samples")

        println("--- START VERIFICATION ---")

        val testItem = SampleItem(
            name = "Verify Item ${kotlin.js.Date().getTime()}",
            createdAt = kotlin.js.Date().toISOString()
        )

        println("1. Writing data: ${testItem.name}")
        val docRef = sampleCollection.add(testItem)
        println("   -> Written ID: ${docRef.id}")

        println("2. Reading data...")
        val querySnapshot = sampleCollection.get()
        val documents = querySnapshot.documents
        println("   -> Fetched ${documents.size} documents.")

        var found = false
        for (doc in documents) {
            val data = runCatching { doc.data<SampleItem>() }.getOrNull() ?: continue
            if (data.name == testItem.name && data.createdAt == testItem.createdAt) {
                found = true
                println("3. Verification SUCCESS: Data match found! [ID: ${doc.id}]")
                break
            }
        }

        if (!found) {
            throw Exception("Verification FAILED: The written data was not found in the fetched results.")
        }
        println("--- VERIFICATION COMPLETE ---")
    }
    process.exit(0)
}
