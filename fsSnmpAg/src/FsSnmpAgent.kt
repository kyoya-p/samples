import com.google.cloud.firestore.Firestore
import firestoreInterOp.firestoreDocumentFlow
import firestoreInterOp.firestoreScopeDefault
import kotlinx.coroutines.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import mibtool.*
import mibtool.snmp4jWrapper.*
import org.snmp4j.Snmp
import java.net.InetAddress
import java.util.*


@ExperimentalCoroutinesApi
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun main(args: Array<String>): Unit = runBlocking {
    val agentId = if (args.isEmpty()) "agent1" else args[0]
    snmpScopeDefault { snmp ->
        firestoreScopeDefault { db ->
            val agent = MfpMibAgent(db, snmp, agentId)
            agent.run()
        }
        null
    }
    println("Terminated.")
}

@ExperimentalCoroutinesApi
class MfpMibAgent(private val db: Firestore, private val snmp: Snmp, private val deviceId: String) {

    @Serializable
    data class AgentRequest(
            val scanAddrSpecs: List<ScanAddrSpec>,
            val autoDetectedRegister: Boolean,
            val time: Long,
    )

    @Serializable
    data class ScanAddrSpec(
            var target: SnmpTarget = SnmpTarget(),
            val broadcastAddr: String? = null,
            val addrUntil: String? = null,
            val schedule: Schedule? = null,
    )

    @Serializable
    data class Schedule(
            val interval: Long,
    )

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

/*    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun run2() = runBlocking {
        val devMap = mutableMapOf<String, Job>()
        db.firestoreDocumentFlow<AgentRequest> { collection("devConfig").document(deviceId) }.collect { req ->
            devMap.forEach { s, job -> job.cancel() }
            devMap.clear()
            println(req)
            detectDevices(req)
                    .toList() // 検索完了まで待つ(手抜き)
                    .distinctBy { "${it.resTarget.addr}/${it.resTarget.port}" } // IPアドレスでの重複を削除
                    .distinctBy { "${it.resPdu.vbl[0].value}:sn=${it.resPdu.vbl[1].value}" } // idでの重複を削除
                    .forEach { res ->
                        val deviceId = "type=mfp.mib:model=${res.resPdu.vbl[0].value}:sn=${res.resPdu.vbl[1].value}"
                        println("Detected Device: ${res.resTarget.addr} $deviceId")
                        val mfp = ProxyMfp(deviceId, res.resTarget)
                        val job = launch { mfp.run() }
                        devMap[deviceId] = job
                    }
        }
    }

 */

    @ExperimentalCoroutinesApi
    suspend fun run() {
        db.firestoreDocumentFlow<AgentRequest> { collection("devConfig").document(deviceId) }
                .collectSchedule {
                    println("${Date().time / 1000} ${it.schedule?.interval}")
                    detectDevices(it).collect {

                    }
                }
    }

    // スケジュールされたタイミングで検索要求を流す
    @ExperimentalCoroutinesApi
    suspend fun Flow<AgentRequest>.collectSchedule(op: suspend Flow<AgentRequest>.(ScanAddrSpec) -> Unit) {
        collectLatest { req ->
            channelFlow {
                req.scanAddrSpecs.forEach { addrSpec ->
                    launch {
                        do {
                            this@channelFlow.offer(addrSpec)
                        } while (addrSpec.schedule?.run { delay(interval);true } == true)
                    }
                }
            }.collect {
                this.op(it)
            }
        }
    }

    // 検索し検索結果を流す
// 重複処理は呼び出し側で行うこと
    fun detectDevices(agentRequest: AgentRequest) = channelFlow {
        val oid_sysName = ".1.3.6.1.2.1.1.1"
        val oid_prtGeneralSerialNumber = ".1.3.6.1.2.1.43.5.1.1.17"
        val sampleOids = listOf(oid_sysName, oid_prtGeneralSerialNumber).map { VB(it) }
        val pdu = PDU.GETNEXT(sampleOids)

        agentRequest.scanAddrSpecs.forEach { addrSpec ->
            addrSpec.broadcastAddr?.let { bcAddr ->
                val target = SnmpTarget()
                snmp.broadcastFlow(pdu.toSnmp4j(), target.toSnmp4j()).collectLatest {
                    offer(it)
                }
            }
            addrSpec.target.addr?.let { startAddr ->
                val endAddr = addrSpec.addrUntil ?: startAddr
                snmp.scanFlow(pdu.toSnmp4j(), addrSpec.target.toSnmp4j(), InetAddress.getByName(endAddr)).collectLatest {
                    offer(it)
                }
            }
        }
        close()
        awaitClose()
    }.map {
        ResponseEvent.from(it)
    }

    suspend fun detectDevices(addrSpec: ScanAddrSpec) = channelFlow {
        val oid_sysName = ".1.3.6.1.2.1.1.1"
        val oid_prtGeneralSerialNumber = ".1.3.6.1.2.1.43.5.1.1.17"
        val sampleOids = listOf(oid_sysName, oid_prtGeneralSerialNumber).map { VB(it) }
        val pdu = PDU.GETNEXT(sampleOids)

        addrSpec.broadcastAddr?.let { bcAddr ->
            val target = SnmpTarget()
            snmp.broadcastFlow(pdu.toSnmp4j(), target.toSnmp4j()).collectLatest {
                offer(it)
            }
        }
        addrSpec.target.addr?.let { startAddr ->
            val endAddr = addrSpec.addrUntil ?: startAddr
            snmp.scanFlow(pdu.toSnmp4j(), addrSpec.target.toSnmp4j(), InetAddress.getByName(endAddr)).collectLatest {
                offer(it)
            }
        }
        close()
        awaitClose()
    }
}
