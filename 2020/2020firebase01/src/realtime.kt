import com.google.cloud.firestore.*

fun main() {
    val db = FirestoreOptions.getDefaultInstance().getService()

    val docRef: DocumentReference = db.collection("agentSettings").document("AG1")
    val registration = docRef.addSnapshotListener(object : EventListener<DocumentSnapshot?> {
        override fun onEvent(snapshot: DocumentSnapshot?,
                             e: FirestoreException?) {
            if (e != null) {
                println("Listen failed: $e")
                return
            }
            if (snapshot != null && snapshot.exists()) {
                println("Current data: " + snapshot.data)
            } else {
                println("Current data: null")
            }
        }
    })

    Thread.sleep(1000 * 30) //30秒間　更新を待つ その間にコンソールからデータを更新

    registration.remove()
    println("Complete.")
}