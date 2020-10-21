import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import firestoreInterOp.from
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
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
import mibtool.snmp4jWrapper.broadcastFlow
import mibtool.snmp4jWrapper.snmpScopeDefault
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
                // 条件に従い検索
                println(it.data)
                val req = AgentRequest2.from(it.data!!)
                devMap.forEach { s, proxyMfp -> proxyMfp.terminate() }
                devMap.clear()
                deviceDetection2(snmp, req).collect { res ->
                    // 検索結果からProxyデバイスを生成
                    val deviceId = "type=mfp.mib:model=${res.pdu.vbl[0].value}:sn=${res.pdu.vbl[1].value}"
                    println("${res.targetAddr} $deviceId")
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
        val detectedDeviceSet = mutableSetOf<String>()
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
                            targetAddr = (ev.peerAddress as UdpAddress).inetAddress.hostAddress,
                            pdu = ev.response.toPDU(),
                            requestTarget = (ev.userObject as CommunityTarget<UdpAddress>?)?.toSnmpTarget()
                    ))
                    //detectedDeviceSet.add()
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
                        "detected" to detectedDeviceSet.toList(),
                        //"removed" to devMap.keys.toList()
                ),
        )
        db.collection("device").document(deviceId).set(agentLog).get() // get() wait complete db access
        db.collection("devLog").document().set(agentLog).get() // get() wait complete db access
        close()
        awaitClose()
    }
}
