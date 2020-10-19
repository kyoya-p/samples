import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import firestoreInterOp.from
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import mibtool.Credential
import mibtool.PDU
import mibtool.Target
import mibtool.snmp4jWrapper.broadcast
import java.util.*
import java.util.concurrent.Semaphore

@Serializable
data class AgentRequest(
        val addrSpec: AddressSpec,
)

@Serializable
data class AddressSpec(
        val broadcastAddr: List<String>, // e.g. ["255.255.255.255"]
        val unicastAddr: List<String>, // e.g. ["192.168.1.1","192.168.100.32"]
        val unicastAddrUntil: List<String>, // e.g. ["192.168.1.255"] ... 192.168.1.1~192.168.1.254
        val credential: Credential,
        val interval: Long,
        val retries: Int,
)


suspend fun main(args: Array<String>) {
    val agentId = if (args.size == 0) "snmp1" else args[0]
    val agent = Agent(agentId)
    agent.run()
    println("Terminated.")
}

class Agent(val deviceId: String) {
    val devMap = mutableMapOf<String, ProxyDevice>()

    fun run() {
        val db = FirestoreOptions.getDefaultInstance().getService()
        val docRef: DocumentReference = db.collection("devSettings").document(deviceId) // 監視するドキュメント
        println("Start listening to Firestore.")
        val smhTerm = Semaphore(1).apply { acquire() }
        docRef.addSnapshotListener(object : EventListener<DocumentSnapshot?> {
            override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) { // ドキュメントRead/Update時ハンドラ
                try {
                    if (ex == null && snapshot != null && snapshot.exists() && snapshot.data != null) {
                        deviceDetection(AgentRequest.from(snapshot.data!!))
                    } else {
                        println("Current data: null")
                        println("Exception: $ex")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
        smhTerm.acquire()
        println("Terminated listening to Firestore.")
    }

    fun deviceDetection(agentRequest: AgentRequest) {
        val startTime = Date().time
        val detectedDeviceMap = mutableMapOf<String, ProxyDevice>()
        val db = FirestoreOptions.getDefaultInstance().getService()

        broadcast(agentRequest.addrSpec.broadcastAddr[0]) { response ->
            if (response != null) {
                //Responsed: 処理結果アップロード
                val key = "type=mfp.mib:model=${response.pdu.vbl[0].value}:sn=${response.pdu.vbl[1].value}"
                if (!devMap.containsKey(key)) {
                    val proxyDev = ProxyDevice(deviceId = key, target = mibtool.Target(addr = response.addr))
                    GlobalScope.launch { proxyDev.run() }
                    devMap[key] = proxyDev
                    detectedDeviceMap.put(key, proxyDev)
                }
            } else {
                //Timeout: 検索終了時のAgentレポート
                val agentLog = mapOf(
                        "time" to startTime,
                        "id" to deviceId,
                        "type" to "agent.mfp.mib",
                        "result" to mapOf(
                                "detected" to detectedDeviceMap.keys.toList(),
                                "removed" to devMap.keys.toList()
                        ),
                )
                db.collection("device").document(deviceId).set(agentLog).get() // get() wait complete db access
                db.collection("devLog").document().set(agentLog).get() // get() wait complete db access
            }
        }
    }
}

class ProxyDevice(val deviceId: String, val target: Target) {
    val db = FirestoreOptions.getDefaultInstance().getService()

    //val smhTerm = Semaphore(1).apply { acquire() }
    fun run() {
        //設定を常にチェック
        val docRef: DocumentReference = db.collection("devSettings").document(deviceId) // 監視ドキュメント
        val registration = docRef.addSnapshotListener(object : EventListener<DocumentSnapshot?> {
            override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) { // ドキュメントRead/Update時ハンドラ
                // 基本的に初期設定情報を取得する。変更あれば再初期化
                if (ex == null && snapshot != null && snapshot.exists() && snapshot.data != null) {
                    println(snapshot.data)
                    // TODO: ここで設定読み込み処理
                }
            }
        })

        while (polling(target)) {
            Thread.sleep(20*60_000)
        }

        println("Start Proxy Device: $deviceId")
        registration.remove()
    }

    fun polling(target: Target): Boolean {
        var lastDeviceStatus = mutableMapOf<String, Any>()
        var alive = false
        // TODO: broadcastではなくUnicast
        broadcast(target.addr) { response ->
            if (response != null) {
                alive = true
                val res = response.pdu.vbl.map { it.value }.joinToString(",")
                println("ProxyDev[$deviceId]: Report: ${res}")
                try {
                    val snmpDeviceLog = mapOf(
                            "time" to Date(),
                            "id" to deviceId,
                            "type" to "mfp.mib",
                            "pdu" to response.pdu,
                    )
                    db.collection("devLog").document().set(snmpDeviceLog).get()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return alive
    }
}