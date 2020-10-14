import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import firestoreInterOp.from
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mibtool.PDU
import mibtool.SnmpConfig
import mibtool.snmp4jWrapper.broadcast
import mibtool.snmp4jWrapper.toPDU
import java.util.*
import java.util.concurrent.Semaphore

@Serializable
data class AgentRequest(
        val addrRangeList: List<AddressRange> = listOf(AddressRange()),
        val filter: List<String> = listOf(".1.3.6.1.2.1.1.1"),
        val report: List<String> = listOf(".1"),
        val snmpConfig: SnmpConfig,
)

@Serializable
data class AddressRange(
        val type: String = "broadcast",
        val addr: String = "255.255.255.255",
        val addrEnd: String = "",
)

fun main() {
    val db = FirestoreOptions.getDefaultInstance().getService()
    val docRef: DocumentReference = db.collection("devSettings").document("snmp1") // 監視するドキュメント

    val semTerm = Semaphore(1).apply { acquire() }
    val registration = docRef.addSnapshotListener(object : EventListener<DocumentSnapshot?> {
        override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) { // ドキュメントRead/Update時ハンドラ
            try {
                val start = Date()

                println(snapshot!!.data)
                if (ex == null && snapshot != null && snapshot.exists()) {
                    val agentRequest = AgentRequest.from(snapshot.data as Map<String, *>)
                    agentAction(agentRequest)
                } else {
                    println("Current data: null")
                }
                println("${start} ~ ${Date()}")

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    })
    println("Start listening to Firestore.")
    semTerm.acquire() //終了指示(release)まで待つ
    registration.remove()

    println("Terminated.")
}

fun agentAction(agentRequest: AgentRequest) = runBlocking {
    val db = FirestoreOptions.getDefaultInstance().getService()

    println(agentRequest.toString())
    broadcast(agentRequest.addrRangeList[0].addr) {
        if (it != null) {
            // 処理結果アップロード
            val res = mapOf(
                    "time" to Date().time,
                    "addr" to it.addr,
                    "pdu" to it.pdu,
            )
            println(res)
            val key = "model=${it.pdu.vbl[0].value}:sn=${it.pdu.vbl[1].value}"
            val res1 = db.collection("device").document(key).set(res)
            val res2 = db.collection("devLog").document().set(res)
            res1.get() //書込み完了待ち
        }
    }
}

fun walk(snmpParam: SnmpConfig, pdu: PDU, addr: String): String {
    val res = mibtool.snmp4jWrapper.walk(snmpParam, pdu, addr).map { it.toPDU().vbl[0] }.toList()
    return Json {}.encodeToString(mapOf("results" to res))
}

