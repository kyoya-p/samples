import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.Serializable
import mibtool.GETNEXT
import mibtool.PDU
import mibtool.SnmpTarget
import mibtool.VB
import mibtool.snmp4jWrapper.toSnmp4j
import mibtool.snmp4jWrapper.sendFlow
import mibtool.snmp4jWrapper.snmpScopeDefault
import mibtool.snmp4jWrapper.toPDU
import java.util.*
import java.util.concurrent.Semaphore

/* MFPデバイスクラス
 実MFPに変わり下記処理を行う
 Firestoreを監視しアクション
 スケジュールに従って情報をアップロード
*/
class ProxyMfp(val deviceId: String, val target: SnmpTarget) {
    @Serializable
    data class Config(
            val time: Long,
            val interval: Long,
    ) //TODO

    @Serializable
    data class Report(
            val time: Long,
            val model: String,
            val sn: String,
            val pdu: PDU,
    ) //TODO


    val db = FirestoreOptions.getDefaultInstance().getService()
    var running = Semaphore(1)

    suspend fun DocumentReference.toEventFlow() = callbackFlow<DocumentSnapshot> {
        val registration = addSnapshotListener(object : EventListener<DocumentSnapshot?> {
            override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) { // ドキュメントRead/Update時ハンドラ
                if (ex == null && snapshot != null && snapshot.exists() && snapshot.data != null) {
                    offer(snapshot)
                } else {
                    //offer(null)
                    close()
                }
            }
        })
        awaitClose { registration.remove() }
    }

    suspend fun run() = snmpScopeDefault { snmp ->
        println("Start Proxy Device: $deviceId")

        channelFlow {
            db.collection("devSettings").document(deviceId).toEventFlow().collect {
                // DBにスケジュール等が登録されていればそれを、なければ interval:60_000 を流す
                offer(it.data)
            }
            val defaultSched = mapOf("interval" to 60_000)
            offer(defaultSched)
            awaitClose()
        }.collect {
            val interval = it?.get("interval") as Int? ?: 60_000
            while (true) {
                val pdu = mibtool.PDU(
                        type = mibtool.PDU.GETNEXT,
                        vbl = listOf(VB(".1.3"))
                ).toSnmp4j()
                snmp.sendFlow(pdu, target.toSnmp4j()).collect { res ->

                    val pdu = res.response.toPDU()
                    val report = Report(
                            time = Date().time,
                            model = pdu.vbl[0].value,
                            sn = pdu.vbl[0].value,
                            pdu = pdu,
                    )
                    db.collection("device").document(deviceId).set(report)
                    db.collection("devLog").document().set(report)
                    println("Sent: $report")
                }
                delay(interval.toLong())
            }
        }
        println("Terminated Proxy Device: $deviceId")
    }

    fun terminate() = running.release()
}
