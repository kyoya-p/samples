package gdvm.agent.mib

import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import mibtool.snmp4jWrapper.*
import mibtool.*
import firestoreInterOp.toJsonObject
import gdvm.device.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import org.snmp4j.Snmp
import java.net.InetAddress
import gdvm.mfp.mib.runMfp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.util.*

val firestore = FirestoreOptions.getDefaultInstance().getService()!!
val snmp = Snmp(DefaultUdpTransportMapping().apply { listen() })

@ExperimentalCoroutinesApi
@Suppress("BlockingMethodInNonBlockingContext")
fun main(args: Array<String>): Unit = runBlocking {
    runCatching {
        if (args.isEmpty()) {
            println("usage: java -jar fsSnmpAg.jar <agentId>")
            return@runCatching -1
        }
        for (agentDeviceId in args) {
            launch { runAgent(agentDeviceId) }
        }
    }.onFailure { it.printStackTrace() }
}


@ExperimentalCoroutinesApi
suspend fun runAgent(agentId: String) = coroutineScope {
    data class DeviceInfo(val id: String, val password: String, val target: SnmpTarget)
    data class Agent(val snapshot: DocumentSnapshot, val data: MfpMibAgentDevice)
    data class Query(val snapshot: DocumentSnapshot, val data: MfpMibAgentQuery, val agent: Agent)

    println("runAgent ${agentId}")
    callbackFlow<Agent> {  // Agent情報をチェックし、存在すれば/更新されれば flow
        val listener = firestore.collection("device").document(agentId)
                .addSnapshotListener(object : EventListener<DocumentSnapshot?> {
                    override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) {
                        println("called: ") //TODO
                        if (ex == null && snapshot != null && snapshot.data != null) {
                            println("called: ${snapshot.data!!.toJsonObject()}") //TODO
                            val ag = Json { ignoreUnknownKeys = true }
                                    .decodeFromJsonElement<MfpMibAgentDevice>(snapshot.data!!.toJsonObject())
                            offer(Agent(snapshot, ag))
                        } else {
                            close()
                        }
                    }
                })
        awaitClose { listener.remove() }
    }.flatMapLatest { agent -> // クエリをチェックし、未処理のクエリがあれば クエリ毎に実行
        println("Agent: $agent")
        val queryCollectionPath = agent.data.dev.notification
                ?: firestore.collection("device").document(agentId).collection("query").path
        callbackFlow<Query> {
            val listener =
                    firestore.collection(queryCollectionPath).addSnapshotListener(object : EventListener<QuerySnapshot?> {
                        override fun onEvent(value: QuerySnapshot?, error: FirestoreException?) {
                            if (error == null && value != null) {
                                for (e in value.documents) {
                                    val query = Json {
                                        ignoreUnknownKeys = true
                                    }.decodeFromJsonElement<MfpMibAgentQuery>(e.data.toJsonObject())
                                    offer(Query(e, query, agent))
                                }
                            } else close()
                        }
                    })
            awaitClose { listener.remove() }
        }
    }.flatMapLatest { query -> // スケジュール時間になれば、次に進む //TODO
        channelFlow {
            query.data.schedule.limit.let {
                repeat(it) {
                    offer(query)
                    delay(query.data.schedule.interval)
                }
            }
        }
    }.flatMapLatest { query -> // SNMP検索し、
        val devSet = mutableSetOf<String>()
        channelFlow<DeviceInfo> {
            launch {
                query.data.scanAddrSpecs.forEach { target ->
                    discoveryDeviceFlow(target, snmp).collect { ev -> // 発見デバイス情報を次に流す
                        val res = ResponseEvent(
                                reqPdu = PDU.from(ev.request), reqTarget = target, resPdu = PDU.from(ev.response),
                                resTarget = SnmpTarget(
                                        addr = ev.peerAddress.inetAddress.hostAddress, port = ev.peerAddress.port,
                                        credential = target.credential, retries = target.retries, interval = target.interval
                                )
                        )
                        println("Detected: ${res.resTarget.addr}")
                        val devId = "type=mfp.mib:model=${res.resPdu.vbl[0].value}:sn=${res.resPdu.vbl[1].value}"
                        if (query.data.autoRegistration) { // 自己デバイス登録
                            firestore.collection("device").document(devId).set(
                                    MfpMibDevice(
                                            id = devId,
                                            cluster = query.agent.data.cluster, // Agentと同じClusterに登録
                                            dev = GdvmDeviceInfo(
                                                    password = query.agent.data.dev.password, // (デフォルトPWのほうがよいだろうか?)
                                                    name = devId,
                                                    ip = ev.peerAddress.inetAddress.hostAddress,
                                                    host = ev.peerAddress.inetAddress.hostAddress,
                                            ),
                                            tags = listOf(
                                                    "detectedBy:$agentId", //TODO
                                                    "cluster:${query.agent.data.cluster}", //TODO
                                            ),
                                    )
                            )
                            // TODO デバイス(の代わりに)レポートを送信。 本来はデバイスの新スタンスが送信する
                            val report = StateSnmpReport(
                                    deviceId = devId,
                                    cluster = query.agent.data.cluster,
                                    pdu = res.resPdu,
                            )
                            firestore.collection("device").document(devId).collection("state").document("snmp").set(report)
                            firestore.collection("device").document(devId).collection("logs").document().set(report)

                            // TODO 仮クエリ設定
                            //firestore.collection("device").document(devId).collection("query").document("default").set()
                        }
                        devSet.add(devId)
                        offer(DeviceInfo(devId, query.agent.data.dev.password, res.resTarget))
                    }
                }
            }.join()
            // 検索結果をlogに記載
            val rep = LogAgentReport(
                    time = Date().time,
                    deviceId = agentId,
                    log = GdvmLog(cluster = query.agent.data.cluster, targets = listOf("device/${agentId}")),
                    result = Result(detected = devSet.toList()),
                    cluster = query.agent.data.cluster,
            )
            query.snapshot.reference.collection("results").document().set(rep)
            query.agent.snapshot.reference.collection("logs").document().set(rep)
        }
    }.collect { devInfo -> //検索されたデバイス毎の処理を起動
        runMfp(devInfo.id, devInfo.password, devInfo.target)
    }
}

// Flow<AgentRequest>を受けスケジュールされたタイミングでAgentRequestを流す
// TODO: 今は一定間隔かワンショットだけ
@ExperimentalCoroutinesApi
suspend fun Flow<MfpMibAgentQuery>.snmpAgentscheduledFlow() = channelFlow {
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
// depends: SNMP4J
@ExperimentalCoroutinesApi
suspend fun discoveryDeviceFlow(target: SnmpTarget, snmp: Snmp) = channelFlow {
    val sampleOids = listOf(hrDeviceDescr, prtGeneralSerialNumber).map { VB(it) }
    val pdu = PDU(GETNEXT, sampleOids)

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
