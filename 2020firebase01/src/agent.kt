import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import kotlinx.coroutines.sync.Semaphore
import java.util.*

val db = FirestoreOptions.getDefaultInstance().getService()

suspend fun main() {
    val semTerm = Semaphore(1).apply { acquire() }

    val docRef: DocumentReference = db.collection("devSettings").document("AG1") // 監視するドキュメント
    val registration = docRef.addSnapshotListener(object : EventListener<DocumentSnapshot?> {
        override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) { // ドキュメント更新時ハンドラ
            if (ex == null && snapshot != null && snapshot.exists()) {
                val start = Date().time
                val reqId = snapshot["requestId"] as String

                // クライアント側処理を実行し結果を生成
                val data: MutableMap<String, Any> = HashMap()
                data["time"] = start
                data["model"] = "MX-XXXX"
                data["sn"] = "123456XY"

                // 処理結果アップロード(Log)
                val res1 = db.collection("ag_logs").document().set(data)

                // 処理結果アップロード(固定のdocにアップロード)
                val res2 = db.collection("device").document("dev1").set(data)

                res1.get() //書込み完了待ち
                res2.get() //書込み完了待ち
                println("${reqId},${start},${Date().time}")
            } else {
                println("Current data: null")
            }
        }
    })

//    Thread.sleep(1000 * 60 * 10) //しばし待つ その間にコンソールからデータを更新して通知
    semTerm.acquire() //終了指示(release)まで待つ

    registration.remove()
    println("Complete.")
}

