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
import ProxyMfp.Request
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

@ExperimentalCoroutinesApi
suspend fun CoroutineScope.runAgent(ac: AppContext, agentId: String) {
    ac.db.firestoreDocumentFlow<AgentRequest> { collection("devConfig").document(agentId) }
            .toScheduleFlow()
            .collectLatest { req ->
                val devSet = mutableSetOf<String>()
                val j = launch {
                    req.scanAddrSpecs.forEach { tg ->
                        discoveryDeviceFlow(tg, ac.snmp).collect { ev ->

                            val res = ResponseEvent.from(ev)

                            //println("${ev.peerAddress}")
                            val devId = "type=mfp.mib:model=${res.resPdu.vbl[0].value}:sn=${res.resPdu.vbl[1].value}"
                            if (!devSet.contains(devId)) {
                                devSet.add(devId)
                                launch {
                                    runMfp(ac, devId, res.resTarget)
                                }
                            }
                        }
                    }
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

@ExperimentalCoroutinesApi
suspend fun CoroutineScope.runMfp(ac: AppContext, deviceId: String, target: SnmpTarget) {
    println("Started Device ${deviceId}")
    try {
        firestoreScopeDefault { db ->
            val oids = listOf(PDU.sysName, PDU.sysDescr, PDU.sysObjectID, PDU.hrDeviceStatus, PDU.hrPrinterStatus, PDU.hrPrinterDetectedErrorState)
            val res = ac.snmp.sendFlow(
                    target = target.toSnmp4j(),
                    pdu = PDU.GETNEXT(vbl = oids.map { VB(it) }).toSnmp4j()
            ).first()

            val rep = ProxyMfp.Report(
                    deviceId = deviceId, type = "mfp.mib", time = Date().time,
                    result = ProxyMfp.Result(
                            pdu = PDU.from(res.response)
                    ),
            )

            db.collection("devLog").document().set(rep).get()
            db.collection("device").document(deviceId).set(rep).get()

//        db.firestoreDocumentFlow<Request> { collection("devConfig").document(deviceId) }.collectLatest {            println(it) }
            delay(10000)
        }
    } catch (e: Exception) {
        println("Exception in $deviceId")
        e.printStackTrace()
    } finally {
        println("Terminated Device ${deviceId}")
    }
}
