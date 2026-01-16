import firebase.app.initializeApp
import firebase.firestore.*
import kotlinx.coroutines.*
import kotlin.js.Promise
import kotlinx.coroutines.await
import node.api.process

fun main() {
    println("Initializing Firebase with environment variables...")
    
    val env = process.env
    val firebaseConfig = js("{}")
    
    // 環境変数から設定を取得 (FB_API_KEY など)
    firebaseConfig.apiKey = env.FB_API_KEY ?: ""
    firebaseConfig.authDomain = env.FB_AUTH_DOMAIN ?: ""
    firebaseConfig.projectId = env.FB_PROJECT_ID ?: ""
    firebaseConfig.storageBucket = env.FB_STORAGE_BUCKET ?: ""
    firebaseConfig.messagingSenderId = env.FB_MESSAGING_SENDER_ID ?: ""
    firebaseConfig.appId = env.FB_APP_ID ?: ""

    if (firebaseConfig.projectId == "") {
        println("Warning: FB_PROJECT_ID is not set. Firebase may not initialize correctly.")
    }

    try {
        val app = initializeApp(firebaseConfig)
        println("Firebase initialized successfully!")
        
        val db = getFirestore(app)
        println("Firestore initialized.")

        GlobalScope.launch {
            try {
                // ダミーデータの書き込み
                val sampleCollection = collection(db, "samples")
                val newDoc = js("{}")
                newDoc.name = "Test Item ${kotlin.js.Date().getTime()}"
                newDoc.createdAt = kotlin.js.Date().toISOString()
                
                println("Adding dummy document: ${JSON.stringify(newDoc)}")
                val docRef = (addDoc(sampleCollection, newDoc) as Promise<dynamic>).await()
                println("Document written with ID: ${docRef.id}")

                // データの読み取り
                println("Fetching data from 'samples' collection...")
                val querySnapshot = (getDocs(sampleCollection) as Promise<dynamic>).await()
                println("Fetched ${querySnapshot.size} documents.")
            } catch (e: dynamic) {
                println("Error during Firestore operation: $e")
            }
        }

    } catch (e: dynamic) {
        println("Error initializing Firebase: $e")
    }
}
