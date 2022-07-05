import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import kotlinx.coroutines.sync.Semaphore
import java.util.*

val db = FirestoreOptions.getDefaultInstance().getService()

suspend fun main() {
    val semTerm = Semaphore(1).apply { acquire() }
    val rnd = Random()

    val docRef: DocumentReference = db.collection("devSettings").document("AG1") // 監視するドキュメント
    val registration = docRef.addSnapshotListener(object : EventListener<DocumentSnapshot?> {
        override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) { // ドキュメント更新時ハンドラ
            if (ex == null && snapshot != null && snapshot.exists()) {
                val start = Date().time

                // クライアント側処理を実行し結果を生成
                val dev = rnd.nextInt(10)
                val model = "MX-R200$dev"
                val sn = "123456XY$dev"
                val data = mapOf<String, Any>("time" to start, "model" to model, "sn" to sn)

                // 処理結果アップロード(Log)
                val res1 = db.collection("ag_logs").document().set(data)

                // 処理結果アップロード(固定のdocにアップロード)
                val res2 = db.collection("devLogs").document("$model:$sn").set(data)

                res1.get() //書込み完了待ち
                res2.get() //書込み完了待ち
                println("${start} ~ ${Date().time}")
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

