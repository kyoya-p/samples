import com.google.cloud.firestore.Firestore
import firestoreInterOp.firestoreDocumentFlow
import firestoreInterOp.firestoreScopeDefault
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import mibtool.*
import mibtool.snmp4jWrapper.*
import org.snmp4j.Snmp
import java.net.InetAddress
import java.util.*

suspend fun main(args: Array<String>) = runBlocking {
    val agentId = if (args.size == 0) "agent1" else args[0]
    snmpScopeDefault { snmp ->
        firestoreScopeDefault { db ->
            val agent = MfpMibAgent(agentId)
            agent.run(snmp, db)
        }
    }
    println("Terminated.")
}

class MfpMibAgent(val deviceId: String) {

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

    suspend fun run2(snmp: Snmp, db: Firestore) = runBlocking {
        val devMap = mutableMapOf<String, Job>()
        db.firestoreDocumentFlow<AgentRequest> { collection("devConfig").document(deviceId) }.collect { req ->
            devMap.forEach { s, job -> job.cancel() }
            devMap.clear()
            println(req)
            detectDevices(snmp, req)
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

    suspend fun run(snmp: Snmp, db: Firestore) {
        if (false) /*TODO:Tmp*/ {
            val v = db.firestoreDocumentFlow<AgentRequest> { collection("devConfig").document(deviceId) }.collectLatest { req ->
                req.scanAddrSpecs[0].scheduledFlow(snmp, db).collect { a ->
                    println("${Date()}: ${req.time} ${a.schedule?.interval}")
                }
            }
        }

        val devMap = mutableMapOf<String, Job>()
        db.firestoreDocumentFlow<AgentRequest> { collection("devConfig").document(deviceId) }
                .launchScheduledFlow(snmp, db).collectLatest { addrSpec ->
                    println("${Date()}: launch schedule: ${addrSpec.schedule?.interval} ${addrSpec.broadcastAddr} ${addrSpec.target.addr}")
                }
    }

    // スケジュールされたタイミングで検索要求を流す
    fun ScanAddrSpec.scheduledFlow(snmp: Snmp, db: Firestore) = channelFlow<ScanAddrSpec> {
        do {
            offer(this@scheduledFlow)
        } while (isActive && schedule?.run { delay(interval);true } ?: false)
        println("Term. Sched.")

        close()
        awaitClose()
    }

    fun Flow<AgentRequest>.launchScheduledFlow(snmp: Snmp, db: Firestore) = channelFlow<ScanAddrSpec> {
        collectLatest { req ->
            req.scanAddrSpecs.forEach { addrSpec ->
                launch {
                    do {
                        offer(addrSpec)
                    } while (isActive && addrSpec.schedule?.run { delay(interval);true } ?: false)
                    println("Term. Sched.")
                }
            }
        }
        close()
        awaitClose()
    }

    // 検索し検索結果を流す
    // 重複処理は呼び出し側で行うこと
    suspend fun detectDevices(snmp: Snmp, agentRequest: AgentRequest) = channelFlow {
        val oid_sysName = ".1.3.6.1.2.1.1.1"
        val oid_prtGeneralSerialNumber = ".1.3.6.1.2.1.43.5.1.1.17"
        val sampleOids = listOf(oid_sysName, oid_prtGeneralSerialNumber).map { VB(it) }
        val pdu = mibtool.PDU.GETNEXT(sampleOids)

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
}
