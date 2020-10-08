package mibtool

import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mibtool.snmp4jWrapper.SnmpParams
import mibtool.snmp4jWrapper.toPDU
import mibtool.snmp4jWrapper.walk
import java.util.*
import java.util.concurrent.Semaphore

// kotlin.serializationでは動的な型(Any等)はシリアライズできないようだ(静的型付け第一か)
//@Serializable
//data class tmp(val param: Map<String, Any /*!*/ >)

fun main() {
    val db = FirestoreOptions.getDefaultInstance().getService()
    val docRef: DocumentReference = db.collection("devSettings").document("snmp1") // 監視するドキュメント

    val semTerm = Semaphore(1).apply { acquire() }
    val registration = docRef.addSnapshotListener(object : EventListener<DocumentSnapshot?> {
        override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) { // ドキュメント更新時ハンドラ
            val start = Date().time
            if (ex == null && snapshot != null && snapshot.exists()) {
                // Firestoreから返されたJsonをsnmpParamに変換
                val request = snapshot.data!!.entries.map { (k, v) -> "\"$k\":\"$v\"" }.joinToString(",", "{", "}")
                println(request)
                val snmpParam = Json {}.decodeFromString<SnmpParams>(request)

                val res = walk(snmpParam).map { it.toPDU() }.toList()

                // 処理結果アップロード(Log)
                val res1 = db.collection("devlogs_a").document().set(Json {}.encodeToString(res))

                // 処理結果アップロード(固定のdocにアップロード)
                //val res2 = db.collection("devLogs").document("model:sn").set(data)

                res1.get() //書込み完了待ち
                //res2.get() //書込み完了待ち
                println("${start} ~ ${Date().time}")
            } else {
                println("Current data: null")
            }
        }
    })
    println("Start listening to Firestore.")
    semTerm.acquire() //終了指示(release)まで待つ
    registration.remove()

    println("Terminated.")
}

