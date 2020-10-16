import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import firestoreInterOp.from
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mibtool.Credential
import mibtool.PDU
import mibtool.SnmpConfig
import mibtool.Target
import mibtool.snmp4jWrapper.broadcast
import mibtool.snmp4jWrapper.toPDU
import java.util.*
import java.util.concurrent.Semaphore

@Serializable
data class AgentRequest(
        val addrSpec: AddressSpec,
        //val filter: List<String>,
        //val report: List<String>,
)

@Serializable
data class AddressSpec(
        val broadcastAddr: List<String>,
        val unicastAddr: List<String>,
        val unicastAddrUntil: List<String>,
        val credential: Credential,
        val interval: Long,
        val retries: Int,
)

@Serializable
data class Filter(
        val pdu: PDU,
        val keyNames: List<String>,
        val keyOids: List<String>,
)

fun main(args: Array<String>) {
    val agentId by lazy { if (args.size == 0) "snmp1" else args[0] }
    val agent = Agent(agentId)
    agent.run()
    println("Terminated.")
}

class Agent(val deviceId: String) {
    val semTerm = Semaphore(1).apply { acquire() }
    val deviceIdMap = mutableMapOf<String, ProxyDevice>()

    fun run() {
        val db = FirestoreOptions.getDefaultInstance().getService()
        val docRef: DocumentReference = db.collection("devSettings").document(deviceId) // 監視するドキュメント

        val registration = docRef.addSnapshotListener(object : EventListener<DocumentSnapshot?> {
            override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) { // ドキュメントRead/Update時ハンドラ
                try {
                    if (ex == null && snapshot != null && snapshot.exists() && snapshot.data != null) {
                        println(snapshot.data)
                        action(AgentRequest.from(snapshot.data!!))
                    } else {
                        println("Current data: null")
                        println("Exception: $ex")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
        println("Start listening to Firestore.")
        semTerm.acquire() //終了指示(release)まで待つ
        registration.remove()
    }

    fun action(agentRequest: AgentRequest) {
        val startTime = Date().time
        val detectedDeviceMap = mutableMapOf<String, ProxyDevice>()
        val db = FirestoreOptions.getDefaultInstance().getService()

        println(agentRequest.toString())
        broadcast(agentRequest.addrSpec.broadcastAddr[0]) { response ->
            if (response != null) {
                // 処理結果アップロード
                val key = "type=mfp.mib:model=${response.pdu.vbl[0].value}:sn=${response.pdu.vbl[1].value}"
                val deviceStatus = mapOf(
                        "time" to startTime,
                        "id" to key,
                        "type" to "mfp.mib",
                        "addr" to response.addr,
                        "pdu" to response.pdu,
                )

                val detected = deviceIdMap.remove(key)
                        ?: ProxyDevice(deviceId = key, target = mibtool.Target(addr = response.addr))
                detectedDeviceMap.put(key, detected)
                println("${response.addr} $key")
                val res1 = db.collection("device").document(key).set(deviceStatus)
                val res2 = db.collection("devLog").document().set(deviceStatus)
                //res1.get() // if wait complete db access
                //res2.get() // if wait complete db access
            } else {
                //Timeout
                val agentLog = mapOf(
                        "time" to startTime,
                        "id" to deviceId,
                        "type" to "agent.mfp.mib",
                        "result" to mapOf(
                                "detected" to detectedDeviceMap.keys.toList(),
                                "removed" to deviceIdMap.keys.toList()
                        ),
                )
                deviceIdMap.values.forEach { it.terminate() } //検知できなかった既存デバイスは終了
                deviceIdMap.clear()
                deviceIdMap.putAll(detectedDeviceMap)

                val res1 = db.collection("device").document(deviceId).set(agentLog)
                val res2 = db.collection("devLog").document().set(agentLog)
                //res1.get() // if wait complete db access
                //res2.get() // if wait complete db access
            }
        }
    }
}

class ProxyDevice(val deviceId: String, val target: Target) {
    val semTerm = Semaphore(1).apply { acquire() }
    fun run() {
        val db = FirestoreOptions.getDefaultInstance().getService()
        val docRef: DocumentReference = db.collection("devSettings").document(deviceId) // 監視ドキュメント

        val registration = docRef.addSnapshotListener(object : EventListener<DocumentSnapshot?> {
            override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) { // ドキュメントRead/Update時ハンドラ
                // 基本的に初期設定情報を取得する。変更あれば再初期化
                try {
                    if (ex == null && snapshot != null && snapshot.exists() && snapshot.data != null) {
                        println(snapshot.data)
                        action(AgentRequest.from(snapshot.data!!))
                    } else {
                        println("Current data: null")
                        println("Exception: $ex")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
        println("Start listening to Firestore.")
        semTerm.acquire()
        registration.remove()
    }

    fun action(agentRequest: AgentRequest) = runBlocking {
    }

    fun terminate() {
        semTerm.release()
    }

    fun walk(snmpParam: SnmpConfig, pdu: PDU, addr: String): String {
        val res = mibtool.snmp4jWrapper.walk(snmpParam, pdu, addr).map { it.toPDU().vbl[0] }.toList()
        return Json {}.encodeToString(mapOf("results" to res))
    }
}

