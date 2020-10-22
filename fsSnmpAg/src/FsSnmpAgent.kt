import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import firestoreInterOp.from
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import mibtool.Credential
import mibtool.ResponseEvent
import mibtool.SnmpTarget
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

    @Serializable
    data class Report(
            val time: Long,
            val deviceId: String,
            val deviceType: String = "agent.mfp.mib",
            val result: Result = Result(),
    )

    @Serializable
    data class Result(
            val detected: List<String> = listOf()
    )

    val devMap = mutableMapOf<String, Job>()

    suspend fun run() = runBlocking {
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
            }.collect { doocSnap ->
                // 条件に従い検索
                println("Received Config: ${doocSnap.data}")
                val req = AgentRequest2.from(doocSnap.data!!)
                devMap.forEach { s, job -> job.cancel() }
                devMap.clear()
                deviceDetection2(snmp, req).collect { res ->
                    // 検索結果からProxyデバイスを生成
                    val deviceId = "type=mfp.mib:model=${res.pdu.vbl[0].value}:sn=${res.pdu.vbl[1].value}"
                    println("Detected Device: ${res.peerAddr} $deviceId")
                    val t = SnmpTarget(
                            addr = res.peerAddr,
                            port = res.requestTarget?.port ?: 0,
                            credential = res.requestTarget?.credential ?: Credential(),
                            retries = res.requestTarget?.retries ?: 0,
                            interval = res.requestTarget?.interval ?: 0,
                    )
                    val mfp = ProxyMfp(deviceId, t)
                    val job = launch { mfp.run() }
                    devMap[deviceId] = job
                }
                // 検索後、検索結果リストをレポート
                val agentLog = Report(
                        time = Date().time,
                        deviceId = deviceId,
                        deviceType = "agent.mfp.mib",
                        result = Result(
                                detected = devMap.keys.toList()
                        ),
                )
                db.collection("device").document(deviceId).set(agentLog)
                db.collection("devLog").document().set(agentLog)

                if (req.autoDetectedRegister) {
                    // TODO:Agentが属するClusterにデバイスを自動登録
                    val s = db.collection("group").document().collection("devices").document(deviceId).get().get()
                    println("Snapshot ${s.data}")
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
                            peerAddr = (ev.peerAddress as UdpAddress).inetAddress.hostAddress,
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

        close()
        awaitClose()
    }
}
