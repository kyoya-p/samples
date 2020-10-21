import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import mibtool.SnmpTarget
import mibtool.snmp4jWrapper.broadcastCB
import snmp4jWrapper.sendFlow
import snmp4jWrapper.snmpScopeDefault
import java.util.*
import java.util.concurrent.Semaphore

/* MFPデバイスクラス
 実MFPに変わり下記処理を行う
 Firestoreを監視しアクション
 スケジュールに従って情報をアップロード
*/
class ProxyMfp(val deviceId: String, val target: SnmpTarget) {
    val db = FirestoreOptions.getDefaultInstance().getService()
    var running = Semaphore(1)

    suspend fun DocumentReference.toEventFlow() = callbackFlow<DocumentSnapshot> {
        val registration = addSnapshotListener(object : EventListener<DocumentSnapshot?> {
            override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) { // ドキュメントRead/Update時ハンドラ
                if (ex == null && snapshot != null && snapshot.exists() && snapshot.data != null) {
                    offer(snapshot)
                } else {
                    close()
                }
            }
        })
        awaitClose { registration.remove() }
    }

    suspend fun run() {
        println("Start Proxy Device: $deviceId")

        snmpScopeDefault { snmp ->
            db.collection("devSettings").document(deviceId).toEventFlow().collect {
                println(it.data)
            }
            while (true) {
                val pdu=PDU()
                snmp.sendFlow(pdu,target.toSnmp4j())
                delay(10_000)
            }
        }
        println("Terminated Proxy Device: $deviceId")
    }

    fun terminate() = running.release()


    fun polling(target: SnmpTarget): Boolean {
        var lastDeviceStatus = mutableMapOf<String, Any>()
        var alive = false
        // TODO: Unicastに変更
        broadcastCB(target.addr) { response ->
            if (response != null) {
                alive = true
                val res = response.pdu.vbl.map { it.value }.joinToString(",")
                try {
                    val snmpDeviceLog = mapOf(
                            "time" to Date().time,
                            "id" to deviceId,
                            "type" to "mfp.mib",
                            "pdu" to response.pdu,
                    )
                    println("ProxyDev[$deviceId]: Report: ${target.addr}")
                    db.collection("device").document(deviceId).set(snmpDeviceLog).get()
                    db.collection("devLog").document().set(snmpDeviceLog).get()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return alive
    }
}
