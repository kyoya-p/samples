package gdvm.agent.mib

import firestoreInterOp.firestoreDocumentFlow
import kotlinx.serialization.Serializable
import mibtool.*
import com.google.cloud.firestore.FirestoreOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import mibtool.snmp4jWrapper.*
import org.snmp4j.Snmp
import java.net.InetAddress
import gdvm.mfp.mib.runMfp
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.util.*

val firestore = FirestoreOptions.getDefaultInstance().getService()!!
val snmp = Snmp(DefaultUdpTransportMapping().apply { listen() })

@ExperimentalCoroutinesApi
@Suppress("BlockingMethodInNonBlockingContext")
fun main(args: Array<String>): Unit = runBlocking {
    runCatching {
        val agentDeviceId = if (args.isEmpty()) "agent1" else args[0]
        println("Start Agent ${agentDeviceId}")
        runAgent2(agentDeviceId)
        println("Terminated Agent ${agentDeviceId}")
    }.onFailure { it.printStackTrace() }
}

@Serializable
data class SnmpAgentDevice(
        val config: SnmpAgentConfig? = null
)

@Serializable
data class SnmpAgentConfig(
        val scanAddrSpecs: List<SnmpTarget>,
        val autoRegistration: Boolean,
        val schedule: Schedule = Schedule(1),
        val time: Long? = null,
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
        val type: String = "agent.mfp.mib",
        val result: Result = Result(),
)

@Serializable
data class Result(
        val detected: List<String> = listOf()
)


@ExperimentalCoroutinesApi
suspend fun runAgent(agentId: String) = coroutineScope {
    firestore.firestoreDocumentFlow<SnmpAgentConfig> { collection("devConfig").document(agentId) }
            .snmpAgentscheduledFlow()
            .collectLatest { req ->
                val devSet = mutableSetOf<String>()
                val j = launch {
                    // 検索とデバイスの生成
                    req.scanAddrSpecs.forEach { tg ->
                        discoveryDeviceFlow(tg, snmp).collect { ev ->
                            val res = ResponseEvent.from(ev)
                            val devId = "type=mfp.mib:model=${res.resPdu.vbl[0].value}:sn=${res.resPdu.vbl[1].value}"
                            if (!devSet.contains(devId)) {
                                devSet.add(devId)
                                launch {
                                    runMfp(devId, res.resTarget)
                                }
                            }
                        }
                    }
                    // 検索結果をレポート
                    val rep = Report(
                            time = Date().time,
                            deviceId = agentId,
                            result = Result(
                                    detected = devSet.toList()
                            ),
                    )
                    firestore.collection("devStatus").document(agentId).set(rep) //.get() //get() is BLocking code

                    // 検索結果をDBに登録
                    // TODO
                    if (req.autoRegistration) {
                        //自身が登録されているGroupを探す(ルール上1つだけ)
                        /* val myGr = firestore.collection("group")
                                .whereEqualTo("a", "a").get() //.get() //TODO: Blocking
                        if (myGr.documents.size == 1) {
                            println(myGr.documents[0].id)
                        }
                         */
                    }
                }
                try {
                    j.join()
                } finally {
                    j.cancelAndJoin()
                }
            }
}


@ExperimentalCoroutinesApi
suspend fun runAgent2(agentId: String) = coroutineScope {
    firestore.firestoreDocumentFlow<SnmpAgentDevice> { collection("device").document(agentId) }
            .map { println(it.config);it.config!! }
            .snmpAgentscheduledFlow()
            .collectLatest { req ->
                val devSet = mutableSetOf<String>()
                val j = launch {
                    // 検索とProxyデバイスの生成
                    req.scanAddrSpecs.forEach { target ->
                        discoveryDeviceFlow(target, snmp).collect { ev ->
                            val res = ResponseEvent.from(ev)
                            val devId = "type=mfp.mib:model=${res.resPdu.vbl[0].value}:sn=${res.resPdu.vbl[1].value}"
                            if (!devSet.contains(devId)) {
                                devSet.add(devId)
                                launch {
                                    runMfp(devId, res.resTarget)
                                }
                            }
                        }
                    }
                    // 検索結果をレポート
                    val rep = Report(
                            time = Date().time,
                            deviceId = agentId,
                            result = Result(
                                    detected = devSet.toList()
                            ),
                    )
                    // 検索結果をDBに登録
                    firestore.collection("device").document(agentId).collection("logs").document().set(rep)
                    firestore.collection("device").document(agentId).collection("state").document("discovery").set(rep)
                    // TODO
                    if (req.autoRegistration) {
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
suspend fun Flow<SnmpAgentConfig>.snmpAgentscheduledFlow() = channelFlow {
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
                InetAddress.getByName(target.addrRangeEnd ?: target.addr) //TODO:BLocking code
        ).collect {
            offer(it)
        }
    }
}
