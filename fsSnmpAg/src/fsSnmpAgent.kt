import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import firestoreInterOp.from
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import mibtool.Credential
import mibtool.Response
import mibtool.Target
import mibtool.snmp4jWrapper.broadcastCB
import mibtool.snmp4jWrapper.broadcastFlow
import mibtool.snmp4jWrapper.scanIpRange
import org.snmp4j.Snmp
import java.net.InetAddress
import java.util.*
import java.util.concurrent.Semaphore

@Serializable
data class AgentRequest(
        val addrSpec: AddressSpec,
)

@Serializable
data class AgentRequest2(
        val addrSpecs: List<AddressSpec2>,
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

@Serializable
data class AddressSpec2(
        val broadcastAddr: String?, // e.g. ["255.255.255.255"]
        val unicastAddr: String?, // e.g. ["192.168.1.1","192.168.100.32"]
        val unicastAddrUntil: String?, // e.g. ["192.168.1.255"] ... 192.168.1.1~192.168.1.254
        val port: Int = 161,
        val credential: Credential,
        val interval: Long = 5000,
        val retries: Int = 5,
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
        val db = FirestoreOptions.getDefaultInstance().getService() //DBインスタンス取得
        val docRef: DocumentReference = db.collection("devSettings").document(deviceId) // 監視するドキュメント
        println("Start listening to Firestore.")
        val smhTerm = Semaphore(1).apply { acquire() }
        docRef.addSnapshotListener(object : EventListener<DocumentSnapshot?> {
            override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) { // ドキュメントRead/Update時ハンドラ
                if (ex == null && snapshot != null && snapshot.exists() && snapshot.data != null) {
                    println("EventListener: ${snapshot.data}")
                    devMap.forEach { s, proxyDevice -> proxyDevice.terminate() }
                    devMap.clear()
                    deviceDetection(AgentRequest.from(snapshot.data!!))
                } else {
                    println("Current data: null")
                    println("FirestoreException: $ex")
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

        agentRequest.addrSpec.broadcastAddr?.forEach { bcAddr ->
            broadcastCB(bcAddr) { response ->
                if (response != null) {
                    //Responsed: 処理結果アップロード
                    val key = "type=mfp.mib:model=${response.pdu.vbl[0].value}:sn=${response.pdu.vbl[1].value}"
                    if (!devMap.containsKey(key)) {
                        val proxyDev = ProxyDevice(deviceId = key, target = mibtool.Target(addr = response.addr))
                        GlobalScope.launch { proxyDev.run() }
                        devMap[key] = proxyDev
                        detectedDeviceMap.put(key, proxyDev)
                    } else {

                    }
                } else {
                    //Timeout: 検索終了時のAgentレポート
                    val agentLog = mapOf(
                            "time" to startTime,
                            "id" to deviceId,
                            "type" to "agent.mfp.mib",
                            "result" to mapOf(
                                    "detected" to detectedDeviceMap.keys.toList(),
                                    //"removed" to devMap.keys.toList()
                            ),
                    )
                    db.collection("device").document(deviceId).set(agentLog).get() // get() wait complete db access
                    db.collection("devLog").document().set(agentLog).get() // get() wait complete db access
                }
            }
        }

        Snmp().use {
            agentRequest.addrSpec.unicastAddr?.forEachIndexed { index, addr ->
                val endAddr = agentRequest.addrSpec.unicastAddrUntil[index];
                scanIpRange(InetAddress.getByName(addr), InetAddress.getByName(endAddr)).forEach {

                }
            }
        }
    }

    /*
    suspend fun deviceDetection2(snmp: Snmp, agentRequest: AgentRequest2) = callbackFlow<Response> {
        val startTime = Date().time
        val detectedDeviceMap = mutableMapOf<String, ProxyDevice>()
        val db = FirestoreOptions.getDefaultInstance().getService()

        agentRequest.addrSpecs.forEach { addrSpec ->
            addrSpec.broadcastAddr?.let {
                broadcastFlow(snmp, it).collect { offer(it) }
            }
            addrSpec.unicastAddr?.let { addr ->
                val endAddr = addrSpec.unicastAddrUntil ?: addr
                scanIpRange(addr, endAddr).forEach {
                    snmp.send()
                }
            }
        }

        val agentLog = mapOf(
                "time" to startTime,
                "id" to deviceId,
                "type" to "agent.mfp.mib",
                "result" to mapOf(
                        "detected" to detectedDeviceMap.keys.toList(),
                        //"removed" to devMap.keys.toList()
                ),
        )
        db.collection("device").document(deviceId).set(agentLog).get() // get() wait complete db access
        db.collection("devLog").document().set(agentLog).get() // get() wait complete db access
    }
}
}*/
}

class ProxyDevice(val deviceId: String, val target: Target) {
    val db = FirestoreOptions.getDefaultInstance().getService()
    var running = Semaphore(1)

    //val smhTerm = Semaphore(1).apply { acquire() }
    fun run() {
        running.acquire()
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

        println("Start Proxy Device: $deviceId")
        while (running.availablePermits() == 0 && polling(target)) {
            Thread.sleep(5 * 60_000)
        }
        println("Terminated Proxy Device: $deviceId")

        registration.remove()
    }

    fun terminate() {
        running.release()
    }

    fun polling(target: Target): Boolean {
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