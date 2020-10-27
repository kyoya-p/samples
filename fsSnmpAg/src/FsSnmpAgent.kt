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
import org.snmp4j.CommunityTarget
import org.snmp4j.Snmp
import org.snmp4j.smi.UdpAddress
import java.net.InetAddress


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

class MfpMibAgent(val db: Firestore, val snmp: Snmp, val deviceId: String) {

    @Serializable
    data class AgentRequest(
            val scanAddrSpecs: List<SnmpTarget>,
            val autoDetectedRegister: Boolean,
            val schedule: Schedule? = null,
            val time: Long,
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

    @ExperimentalCoroutinesApi
    suspend fun run() = runBlocking {
        db.firestoreDocumentFlow<AgentRequest> { collection("devConfig").document(deviceId) }
                .collectSchedule {
                    val detectedList = detectDevices(it)
                            .toList()
                            .distinctBy { it.peerAddress }
                            .distinctBy { "${it.response[0].toString()}/${it.response[1].toString()}" }
                    detectedList.forEach { ev ->
                        // println("${Date()} ${it.peerAddress} ${it.response[0].toString()}/${it.response[1].toString()} ")
                        val deviceId = "type=mfp.mib:model=${ev.response[0].variable}:sn=${ev.response[1].variable}"
                        val reqTarget = (ev.userObject as CommunityTarget<UdpAddress>).apply {
                            address = ev.peerAddress
                        }
                        val mfp = ProxyMfp(db, snmp, deviceId, SnmpTarget.from(reqTarget))
                        launch { mfp.run() }
                    }
                }
    }

    // スケジュールされたタイミングで検索要求を流す
    @ExperimentalCoroutinesApi
    suspend fun Flow<AgentRequest>.collectSchedule(op: suspend Flow<AgentRequest>.(SnmpTarget) -> Unit) {
        println("Start collectSchedule()")
        try {
            collectLatest { req ->
                channelFlow {
                    do {
                        req.scanAddrSpecs.forEach { target ->
                            launch {
                                this@channelFlow.offer(target)
                            }
                        }
                    } while (req.schedule?.run { delay(interval);true } == true)
                }.collect {
                    this.op(it)
                }
            }
        } finally {
            println("Term. collectSchedule()")
        }
    }

    suspend fun detectDevices(target: SnmpTarget) = channelFlow {
        val oid_sysName = ".1.3.6.1.2.1.1.1"
        val oid_prtGeneralSerialNumber = ".1.3.6.1.2.1.43.5.1.1.17"
        val sampleOids = listOf(oid_sysName, oid_prtGeneralSerialNumber).map { VB(it) }
        val pdu = PDU.GETNEXT(sampleOids)

        if (target.isBroadcast) {
            snmp.broadcastFlow(pdu.toSnmp4j(), target.toSnmp4j()).collectLatest {
                offer(it)
            }
        } else {
            snmp.scanFlow(
                    pdu.toSnmp4j(),
                    target.toSnmp4j(),
                    InetAddress.getByName(target.endAddr ?: target.addr)
            ).collectLatest {
                offer(it)
            }
        }
        close()
        awaitClose()
    }
}
