import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.initialize
import kotlinx.coroutines.*
import node.api.process
import com.example.model.SampleItem

fun main() {
    // Force load firestore module for Node.js environment
    // Required because modular SDK doesn't register components globally by default
    try {
        js("require('firebase/firestore')")
    } catch (e: dynamic) {
        println("Note: manual require('firebase/firestore') failed: $e")
    }

    println("Initializing Firebase with provided config...")
    
    val env = process.env
    
    val apiKey = env.FB_API_KEY as? String ?: ""
    if (apiKey.isEmpty()) {
        println("WARNING: FB_API_KEY is not set or empty.")
    } else {
        println("FB_API_KEY is set (length: ${apiKey.length})")
    }

    val options = FirebaseOptions(
        apiKey = apiKey,
        authDomain = env.FB_AUTH_DOMAIN as? String,
        projectId = env.FB_PROJECT_ID as? String ?: "riot26-70125",
        storageBucket = env.FB_STORAGE_BUCKET as? String,
        // messagingSenderId = env.FB_MESSAGING_SENDER_ID as? String,
        applicationId = env.FB_APP_ID as? String ?: ""
    )

    try {
        Firebase.initialize(context = null, options = options)
        println("Firebase initialized successfully!")
        
        val db = Firebase.firestore
        println("Firestore initialized.")

        GlobalScope.launch {
            try {
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
                        try {
                            val data = doc.data<SampleItem>()
                            if (data.name == testItem.name && data.createdAt == testItem.createdAt) {
                                found = true
                                println("3. Verification SUCCESS: Data match found! [ID: ${doc.id}]")
                                break
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    
                    if (!found) {
                        throw Exception("Verification FAILED: The written data was not found in the fetched results.")
                    }
                    
                    println("--- VERIFICATION COMPLETE ---")
                }
                process.exit(0)
                
            } catch (e: TimeoutCancellationException) {
                println("Error: Operation timed out after 15 seconds!")
                process.exit(1)
            } catch (e: Throwable) {
                println("Error during Firestore operation: $e")
                e.printStackTrace()
                process.exit(1)
            }
        }

    } catch (e: Throwable) {
        println("Error initializing Firebase: $e")
        e.printStackTrace()
    }
}