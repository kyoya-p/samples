package agent.mib

import kotlinx.serialization.Serializable
import mibtool.*
import com.google.cloud.firestore.Firestore
import firestoreInterOp.firestoreDocumentFlow
import firestoreInterOp.firestoreScopeDefault
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import mibtool.snmp4jWrapper.*
import org.snmp4j.Snmp
import java.net.InetAddress
import mfp.mib.runMfp
import java.util.*

data class AppContext(
        val db: Firestore,
        val snmp: Snmp,
)

@ExperimentalCoroutinesApi
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun main(args: Array<String>): Unit = runBlocking {
    val agentDeviceId = if (args.isEmpty()) "agent1" else args[0]
    snmpScopeDefault { snmp ->
        firestoreScopeDefault { db ->
            runAgent(AppContext(db, snmp), agentId = agentDeviceId)
        }
    }
}

@Serializable
data class AgentRequest(
        val scanAddrSpecs: List<SnmpTarget>,
        val autoDetectedRegister: Boolean,
        val schedule: Schedule = Schedule(1),
        val time: Long,
)

@Serializable
data class Schedule(
        val limit: Int = 1, //　回数は有限に。失敗すると破産するし
        val interval: Long = 0,
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
suspend fun runAgent(ac: AppContext, agentId: String) = coroutineScope {
    ac.db.firestoreDocumentFlow<AgentRequest> { collection("devConfig").document(agentId) }
            .toScheduleFlow()
            .collectLatest { req ->
                val devSet = mutableSetOf<String>()
                val j = launch {
                    req.scanAddrSpecs.forEach { tg ->
                        discoveryDeviceFlow(tg, ac.snmp).collect { ev ->
                            val res = ResponseEvent.from(ev)
                            val devId = "type=mfp.mib:model=${res.resPdu.vbl[0].value}:sn=${res.resPdu.vbl[1].value}"
                            if (!devSet.contains(devId)) {
                                devSet.add(devId)
                                launch {
                                    runMfp(ac, devId, res.resTarget)
                                }
                            }
                        }
                    }
                    val rep = Report(
                            time = Date().time,
                            deviceId = agentId,
                            result = Result(
                                    detected = devSet.toList()
                            ),
                    )
                    ac.db.collection("device").document(agentId).set(rep).get()
                }
                try {
                    j.join()
                } finally {
                    j.cancel()
                    j.join()
                }
            }
}


// Flow<AgentRequest>を受けスケジュールされたタイミングでAgentRequestを流す
// TODO: 今は一定間隔かワンショットだけ
@ExperimentalCoroutinesApi
suspend fun Flow<AgentRequest>.toScheduleFlow() = channelFlow {
    collectLatest { req ->
        try {
            repeat(req.schedule.limit) {
                offer(req)
                delay(req.schedule.interval)
            }
        } finally {
            println("terminate toScheduleFlow()")
        }
    }
    close()
    awaitClose()
}

// 指定の条件でSNMP検索し、検索結果を流す
@ExperimentalCoroutinesApi
suspend fun discoveryDeviceFlow(target: SnmpTarget, snmp: Snmp) = channelFlow {
    val sampleOids = listOf(PDU.hrDeviceDescr, PDU.prtGeneralSerialNumber).map { VB(it) }
    val pdu = PDU.GETNEXT(sampleOids)

    if (target.isBroadcast) {
        snmp.broadcastFlow(pdu.toSnmp4j(), target.toSnmp4j()).collect {
            offer(it)
        }
    } else {
        snmp.scanFlow(
                pdu.toSnmp4j(),
                target.toSnmp4j(),
                InetAddress.getByName(target.addrRangeEnd ?: target.addr)
        ).collect {
            offer(it)
        }
    }
}
