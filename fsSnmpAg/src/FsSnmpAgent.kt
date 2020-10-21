import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import firestoreInterOp.from
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import mibtool.Credential
import mibtool.ResponseEvent
import mibtool.SnmpTarget
import mibtool.snmp4jWrapper.broadcastCB
import mibtool.snmp4jWrapper.scanIpRange
import mibtool.snmp4jWrapper.toPDU
import mibtool.snmp4jWrapper.toSnmpTarget
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.smi.OID
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import snmp4jWrapper.broadcastFlow
import snmp4jWrapper.sendFlow
import snmp4jWrapper.snmpScopeDefault
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
        val autoDetectedRegister: Boolean,
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
    val agent = MfpMibAgent(agentId)
    agent.run()
    println("Terminated.")
}

class MfpMibAgent(val deviceId: String) {
    val devMap = mutableMapOf<String, ProxyMfp>()

    suspend fun run() {
        snmpScopeDefault { snmp ->
            val db = FirestoreOptions.getDefaultInstance().getService() //DBインスタンス取得
            val docRef: DocumentReference = db.collection("devSettings").document(deviceId) // 監視するドキュメント
            println("Start listening to Firestore.")
            callbackFlow<DocumentSnapshot> { // Firestoreからのイベントを受信しFlowに流す
                val listener = docRef.addSnapshotListener(object : EventListener<DocumentSnapshot?> {
                    override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) { // ドキュメントRead/Update時ハンドラ
                        if (ex == null && snapshot != null && snapshot.exists() && snapshot.data != null) offer(snapshot)
                        else {
                            val errLog = mapOf(
                                    "time" to Date().time,
                                    "id" to deviceId,
                                    "type" to "agent.mfp.mib",
                                    "status" to "error.noSettingDocument"
                            )
                            db.collection("devLog").document().set(errLog).get()
                            close()
                        }
                    }
                })
                awaitClose { listener.remove() }
            }.collect {
                println(it.data)
                val req = AgentRequest2.from(it.data!!)
                devMap.forEach { s, proxyMfp -> proxyMfp.terminate() }
                devMap.clear()
                deviceDetection2(snmp, req).collect { res ->
                    val deviceId = "type=mfp.mib:model=${res.pdu.vbl[0].value}:sn=${res.pdu.vbl[1].value}"
                    println("${res.addr} $deviceId")
                    val mfp = ProxyMfp(deviceId, res.requestTarget!!)
                    mfp.run()
                    devMap[deviceId] = mfp
                }
            }
        }
        println("Terminated listening to Firestore.")
    }

    suspend fun deviceDetection2(snmp: Snmp, agentRequest: AgentRequest2) = callbackFlow<ResponseEvent> {
        val startTime = Date().time
        val detectedDeviceMap = mutableMapOf<String, ProxyMfp>()
        val db = FirestoreOptions.getDefaultInstance().getService()

        agentRequest.addrSpecs.forEach { addrSpec ->
            val oid_sysName = ".1.3.6.1.2.1.1.1"
            val oid_prtGeneralSerialNumber = ".1.3.6.1.2.1.43.5.1.1.17"
            val sampleOids = listOf(oid_sysName, oid_prtGeneralSerialNumber)
            val pdu = PDU(PDU.GETNEXT, sampleOids.map { VariableBinding(OID(it)) })
            addrSpec.broadcastAddr?.let { addr ->
                val target = CommunityTarget(
                        UdpAddress(InetAddress.getByName(addr), 161),
                        OctetString("public"),
                )
                snmp.broadcastFlow(pdu, target, target).collect { ev ->
                    offer(ResponseEvent(
                            addr = (ev.peerAddress as UdpAddress).inetAddress.hostAddress,
                            pdu = ev.response.toPDU(),
                            requestTarget = (ev.userObject as CommunityTarget<UdpAddress>?)?.toSnmpTarget()
                    ))
                }
            }
            addrSpec.unicastAddr?.let { startAddr ->
                val endAddr = addrSpec.unicastAddrUntil ?: startAddr
                scanIpRange(startAddr, endAddr).forEach { addr ->
                    val target = CommunityTarget(
                            UdpAddress(InetAddress.getByName((addr as UdpAddress).inetAddress.hostAddress), 161),
                            OctetString("public"),
                    )
//                    snmp.sendFlow(pdu, target)
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
        close()
        awaitClose()
    }
}


class ProxyDeviceOld(val deviceId: String, val target: SnmpTarget) {
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

    fun polling(target: SnmpTarget): Boolean {
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