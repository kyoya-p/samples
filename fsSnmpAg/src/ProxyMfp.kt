import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.Serializable
import mibtool.GETNEXT
import mibtool.PDU
import mibtool.SnmpTarget
import mibtool.VB
import mibtool.snmp4jWrapper.sendFlow
import mibtool.snmp4jWrapper.snmpScopeDefault
import mibtool.snmp4jWrapper.toPDU
import mibtool.snmp4jWrapper.toSnmp4j
import org.snmp4j.Snmp
import java.util.*

/* MFPデバイスクラス
 実MFPに変わり処理を行う
 Firestoreを監視しアクション
 スケジュールに従って情報をアップロード
*/
class ProxyMfp(val deviceId: String, val target: SnmpTarget) {

    @Serializable
    data class Report(
            val time: Long = Date().time,
            val deviceId: String,
            val result: Result,
    )

    @Serializable
    data class Result(
            val pdu: PDU,
            val target: SnmpTarget,
    )

    val db = FirestoreOptions.getDefaultInstance().getService()
    //var running = Semaphore(1)

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

    suspend fun sendReport(snmp: Snmp) {

        val pdu = PDU.GETNEXT(vbl = listOf(VB(".1.3.6"))).toSnmp4j()
        snmp.sendFlow(pdu, target.toSnmp4j()).collect { res ->
            println("Device Recv: ${res.response}")
            val r = Report(
                    deviceId = deviceId,
                    result = Result(
                            target = SnmpTarget(
                                    addr = res.peerAddress.inetAddress.hostAddress,
                                    port = res.peerAddress.port
                            ),
                            pdu = res.response.toPDU(),
                    ),
            )
            db.collection("devLog").document().set(r)
            db.collection("device").document(deviceId).set(r)
        }
    }

    suspend fun run() = runBlocking {
        println("Start Proxy Device: $deviceId")
        snmpScopeDefault { snmp ->
            channelFlow { // TODO: もう少しマシなスケジュール設定を流す (今はintervalのみ)
                db.collection("devSettings").document(deviceId).toEventFlow().collect { snapshot ->
                    val interval = snapshot.data!!["interval"] as Int
                    offer(interval.toLong())
                }
                offer(20*60_000L) // 取得できなかった場合のインターバル
            }.collect { interval ->
                // 定期的にレポートをアップロード
                while (isActive) {
                    sendReport(snmp)
                    delay(interval)
                }
            }
        }
        println("Terminated Proxy Device: $deviceId")
    }

}
