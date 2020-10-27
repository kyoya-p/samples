import com.google.cloud.firestore.Firestore
import firestoreInterOp.firestoreDocumentFlow
import firestoreInterOp.firestoreScopeDefault
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import mibtool.*
import mibtool.snmp4jWrapper.*
import org.snmp4j.Snmp
import java.net.InetAddress
import MfpMibAgent.AgentRequest

data class AgentContext(
        val db: Firestore,
        val snmp: Snmp,
)

@ExperimentalCoroutinesApi
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun main(args: Array<String>): Unit = runBlocking {
    val agentDeviceId = if (args.isEmpty()) "agent1" else args[0]
    snmpScopeDefault { snmp ->
        firestoreScopeDefault { db ->
            runAgent(AgentContext(db, snmp), deviceId = agentDeviceId)
        }
        null
    }
    println("Terminated.")
}

@ExperimentalCoroutinesApi
suspend fun CoroutineScope.runAgent(ag: AgentContext, deviceId: String) {
    ag.db.firestoreDocumentFlow<AgentRequest> { collection("devConfig").document(deviceId) }
            .toScheduleFlow()
            //.toDiscoveryDeviceFlow(ag.snmp)
            .collectLatest { ev ->
                try {
                    flow { repeat(2) { emit(it) } }.collect {

                    }
                } finally {
                    println("Terminate terminal-task.")
                }
            }
    /*
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

     */
}

// Flow<AgentRequest>を受けスケジュールされたタイミングでAgentRequestを流す
// TODO: 今は一定間隔かワンショットだけ
@ExperimentalCoroutinesApi
suspend fun Flow<AgentRequest>.toScheduleFlow() = channelFlow {
    collectLatest { req ->
        try {
            do {
                offer(req)
            } while (req.schedule?.run { delay(interval);true } == true)
        } finally {
            println("terminate toScheduleFlow()")
        }
    }
    close()
    awaitClose()
}

// Flow<AgentRequest>を受け、指定の条件でSNMP検索し、検索結果を流す
@ExperimentalCoroutinesApi
suspend fun Flow<AgentRequest>.toDiscoveryDeviceFlow(snmp: Snmp) = channelFlow {
    val oid_sysName = ".1.3.6.1.2.1.1.1"
    val oid_prtGeneralSerialNumber = ".1.3.6.1.2.1.43.5.1.1.17"
    val sampleOids = listOf(oid_sysName, oid_prtGeneralSerialNumber).map { VB(it) }
    val pdu = PDU.GETNEXT(sampleOids)

    collectLatest {
        it.scanAddrSpecs.forEach { target ->
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
        }
    }
    close()
    awaitClose()
}

