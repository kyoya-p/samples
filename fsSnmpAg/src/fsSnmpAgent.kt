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


fun main(args: Array<String>) {
    val agentId by lazy { if (args.size == 0) "snmp1" else args[0] }
    val db = FirestoreOptions.getDefaultInstance().getService()
    val docRef: DocumentReference = db.collection("devSettings").document(agentId) // 監視するドキュメント

    val semTerm = Semaphore(1).apply { acquire() }
    val registration = docRef.addSnapshotListener(object : EventListener<DocumentSnapshot?> {
        override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) { // ドキュメントRead/Update時ハンドラ
            try {
                val start = Date()
                if (ex == null && snapshot != null && snapshot.exists() && snapshot.data != null) {
                    println(snapshot.data)
                    val agentRequest = AgentRequest.from(snapshot.data!!)
                    val agent=Agent(agentId)
                    agent.action(agentRequest)
                } else {
                    println("Current data: null")
                    println("Exception: $ex")
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

class Agent(val agentId:String) {
    fun action(agentRequest: AgentRequest) = runBlocking {
        val startTime = Date().time
        val setDetected = mutableSetOf<String>()
        val db = FirestoreOptions.getDefaultInstance().getService()

        println(agentRequest.toString())
        broadcast(agentRequest.addrRangeList[0].addr) { response ->
            if (response != null) {
                // 処理結果アップロード
                val key = "model=${response.pdu.vbl[0].value}:sn=${response.pdu.vbl[1].value}"
                setDetected.add(key)
                val deviceStatus = mapOf(
                        "time" to startTime,
                        "id" to key,
                        "addr" to response.addr,
                        "pdu" to response.pdu,
                )

                println(key)
                val res1 = db.collection("device").document(key).set(deviceStatus)
                val res2 = db.collection("devLog").document().set(deviceStatus)
                //res1.get() //書込み完了待ち
                //res2.get() //書込み完了待ち
            } else {
                //Timeout
                val agentLog = mapOf(
                        "time" to startTime,
                        "id" to agentId,
                        "result" to mapOf("detected" to setDetected.toList())
                )
                println(agentLog)

                val res3 = db.collection("devLog").document().set(agentLog)
                //res3.get() //
            }
        }
    }

    fun walk(snmpParam: SnmpConfig, pdu: PDU, addr: String): String {
        val res = mibtool.snmp4jWrapper.walk(snmpParam, pdu, addr).map { it.toPDU().vbl[0] }.toList()
        return Json {}.encodeToString(mapOf("results" to res))
    }
}

