import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import firestoreInterOp.firestoreDocumentFlow
import firestoreInterOp.firestoreEventFlow
import firestoreInterOp.firestoreScopeDefault
import kotlinx.coroutines.*
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
class ProxyMfp (val db: Firestore, val snmp: Snmp, val deviceId: String, val target: SnmpTarget) {

    @Serializable
    data class Request(
            // TODO: スケジュール設定
            val interval: Long = 1 * 60_000,
    )

    @Serializable
    data class Report(
            val deviceId: String,
            val type: String = "mfp.mib",
            val time: Long = Date().time,
            val result: Result,
    )

    @Serializable
    data class Result(
            val pdu: PDU,
    )

//    val db = FirestoreOptions.getDefaultInstance().getService()

    suspend fun run() = runBlocking {
        println("Start Proxy Device: $deviceId ${target.addr}")
        snmpScopeDefault { snmp ->
            firestoreScopeDefault { db ->
                channelFlow {
                    db.firestoreDocumentFlow<Request> { collection("devConfig").document(deviceId) }.collect { it -> offer(it) }
                    offer(Request()) //取得できなかった場合はデフォルトのスケジュールを流す
                }.collect { req ->
                    println(req)
                    while (isActive) { // とりあえず定期的にレポートをアップロード
                        sendReport(snmp, db)
                        delay(req.interval)
                    }
                    cancel()
                }
            }
        }
        println("Terminated Proxy Device: $deviceId ${target.addr}")
    }

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

    suspend fun sendReport(snmp: Snmp, db: Firestore) {
        val pdu = PDU.GETNEXT(vbl = listOf(VB(".1.3.6"))).toSnmp4j()
        snmp.sendFlow(pdu, target.toSnmp4j()).collect { res ->
            println("Device Recv: ${res.response}")
            val r = Report(
                    deviceId = deviceId,
                    result = Result(
                            pdu = res.response.toPDU(),
                    ),
            )
            db.collection("devLog").document().set(r)
            db.collection("device").document(deviceId).set(r)
        }
    }


}
