package fssnmpagent

import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import mibtool.PDU
import mibtool.Response
import mibtool.SnmpConfig
import mibtool.VB
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
            val start = Date().time
            if (ex == null && snapshot != null && snapshot.exists()) {
                agentAction(snapshot)
            } else {
                println("Current data: null")
            }
            println("${start} ~ ${Date().time}")
        }
    })
    println("Start listening to Firestore.")
    semTerm.acquire() //終了指示(release)まで待つ
    registration.remove()

    println("Terminated.")
}

fun agentAction(snapshot: DocumentSnapshot) = runBlocking {
    // Firestoreから返されたJsonをsnmpParamに変換
//                val request = snapshot.data!!.entries.map { (k, v) -> "\"$k\":\"$v\"" }.joinToString(",", "{", "}")
    val m = snapshot.data!!.entries.mapNotNull { (k, v) -> if (k != null && v as JsonElement? != null) k to v else null }.toMap()
//    val request1 = JsonObject(m)
//    val request= Json{}.decodeFromString<AgentRequest>(snapshot.data.toString())
//    println(request)
//                val agentRequest = Json {}.decodeFromString<AgentRequest>(snapshot.data)
//                val agentRequest = Json {}.encodeToString(JsonElement.serializer(), snapshot.data)
    val agentRequest = snapshot.data!!.toString()
    println(agentRequest.toString())
//                agentAction(agentRequest)

    broadcast("255.255.255.255") {

    }

    // 処理結果アップロード(Log)
    val res1 = db.collection("devlogs_a").document().set("res")
    res1.get() //書込み完了待ち

}

fun walk(snmpParam: SnmpConfig, pdu: PDU, addr: String): String {
    val res = mibtool.snmp4jWrapper.walk(snmpParam, pdu, addr).map { it.toPDU().vbl[0] }.toList()
    return Json {}.encodeToString(mapOf("results" to res))
}

