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

val db = FirestoreOptions.getDefaultInstance().getService()

fun main() {
    val docRef: DocumentReference = db.collection("devSettings").document("snmp1") // 監視するドキュメント

    val semTerm = Semaphore(1).apply { acquire() }
    val registration = docRef.addSnapshotListener(object : EventListener<DocumentSnapshot?> {
        override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) { // ドキュメント更新時ハンドラ
            try {
                val start = Date().time

                println(snapshot!!.data)
                if (ex == null && snapshot != null && snapshot.exists()) {
                    val agentRequest = AgentRequest.from(snapshot.data as Map<String, *>)
                    agentAction(agentRequest)
                } else {
                    println("Current data: null")
                }
                println("${start} ~ ${Date().time}")

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
    println(agentRequest.toString())
    broadcast("255.255.255.255") {

    }

    // 処理結果アップロード(Log)
    val res1 = db.collection("devlogs_a").document().set(mapOf("res" to "res"))
    res1.get() //書込み完了待ち

}

fun walk(snmpParam: SnmpConfig, pdu: PDU, addr: String): String {
    val res = mibtool.snmp4jWrapper.walk(snmpParam, pdu, addr).map { it.toPDU().vbl[0] }.toList()
    return Json {}.encodeToString(mapOf("results" to res))
}

