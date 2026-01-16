import firebase.app.initializeApp
import firebase.firestore.*
import kotlinx.coroutines.*
import kotlin.js.Promise
import kotlinx.coroutines.await
import node.api.process
import com.example.model.SampleItem

fun main() {
    println("Initializing Firebase with provided config...")
    
    val env = process.env
    val firebaseConfig = js("{}")
    
    // 環境変数の読み込み確認ログ
    val apiKey = env.FB_API_KEY as? String ?: ""
    if (apiKey.isEmpty()) {
        println("WARNING: FB_API_KEY is not set or empty.")
    } else {
        println("FB_API_KEY is set (length: ${apiKey.length})")
    }

    firebaseConfig.apiKey = apiKey
    firebaseConfig.authDomain = env.FB_AUTH_DOMAIN ?: ""
    firebaseConfig.projectId = env.FB_PROJECT_ID ?: "riot26-70125"
    firebaseConfig.storageBucket = env.FB_STORAGE_BUCKET ?: ""
    firebaseConfig.messagingSenderId = env.FB_MESSAGING_SENDER_ID ?: ""
    firebaseConfig.appId = env.FB_APP_ID ?: ""

    try {
        val app = initializeApp(firebaseConfig)
        println("Firebase initialized successfully!")
        
        val db = getFirestore(app)
        println("Firestore initialized.")

        GlobalScope.launch {
            try {
                // 15秒のタイムアウトを設定
                withTimeout(15000) {
                    val sampleCollection = collection(db, "samples")
                    
                    println("--- START VERIFICATION ---")

                    // shared モジュールのデータモデルを使用
                    val testItem = SampleItem(
                        name = "Verify Item ${kotlin.js.Date().getTime()}",
                        createdAt = kotlin.js.Date().toISOString()
                    )
                    
                    // 1. 書き込み
                    println("1. Writing data: ${testItem.name}")
                    val newDoc = js("{}")
                    newDoc.name = testItem.name
                    newDoc.createdAt = testItem.createdAt
                    
                    val docRef = (addDoc(sampleCollection, newDoc) as Promise<dynamic>).await()
                    println("   -> Written ID: ${docRef.id}")

                    // 2. 読み出し
                    println("2. Reading data...")
                    val querySnapshot = (getDocs(sampleCollection) as Promise<dynamic>).await()
                    val docs: Array<dynamic> = querySnapshot.docs // docs プロパティにアクセス
                    println("   -> Fetched ${querySnapshot.size} documents.")
                    
                    // 3. 検証
                    var found = false
                    // querySnapshot.forEach は Kotlin からは使いにくい場合があるため、docs配列をループ
                    for (i in 0 until docs.size) {
                        val doc = docs[i]
                        val data = doc.data()
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
                
            } catch (e: TimeoutCancellationException) {
                println("Error: Operation timed out after 15 seconds!")
                process.exit(1)
            } catch (e: dynamic) {
                println("Error during Firestore operation: $e")
                process.exit(1)
            }
        }

    } catch (e: dynamic) {
        println("Error initializing Firebase: $e")
    }
}
