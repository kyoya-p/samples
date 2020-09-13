import com.google.api.core.ApiFuture
import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import com.google.type.DateTime
import java.util.*

val db = FirestoreOptions.getDefaultInstance().getService()

fun main() {
    val docRef: DocumentReference = db.collection("agentSettings").document("AG1")
    val registration = docRef.addSnapshotListener(object : EventListener<DocumentSnapshot?> {
        override fun onEvent(snapshot: DocumentSnapshot?, e: FirestoreException?) {
            if (e != null) {
                println("Listen failed: $e")
                return
            }
            if (snapshot != null && snapshot.exists()) {
                println("Current data: " + snapshot.data)
                val docRef: DocumentReference = db.collection("ag_logs").document()
                val data: MutableMap<String, Any> = HashMap()
                data["time"] = Date().toString()
                docRef.set(data)
            } else {
                println("Current data: null")
            }
        }
    })

    Thread.sleep(1000 * 30) //30秒間　更新を待つ その間にコンソールからデータを更新

    registration.remove()
    println("Complete.")
}

