package gdvm.agent.mib

import com.google.cloud.firestore.*
import com.google.cloud.firestore.EventListener
import mibtool.*
import firestoreInterOp.toJsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import mibtool.snmp4jWrapper.*
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
        val agentDeviceIds = if (args.isEmpty()) arrayOf("agent1") else args
        for (agentDeviceId in agentDeviceIds) {
            launch {
                println("Start Agent ${agentDeviceId}")
                runAgent(agentDeviceId)
                println("Terminated Agent ${agentDeviceId}")
            }
        }
    }.onFailure { it.printStackTrace() }
}


@ExperimentalCoroutinesApi
suspend fun runAgent(agentId: String) = coroutineScope {
    data class DeviceInfo(val id: String, val password: String, val target: SnmpTarget) //TODO:
    data class Work(val agent: MfpMibAgentDevice, val query: MfpMibAgentQuery)
    callbackFlow<MfpMibAgentDevice> {  // Agent情報をチェックし変更あればflow
        val listener = firestore.collection("device").document(agentId).addSnapshotListener(object : EventListener<DocumentSnapshot?> {
            override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) {
                if (ex == null && snapshot != null && snapshot.exists() && snapshot.data != null) {
                    val ag = Json { ignoreUnknownKeys = true }.decodeFromJsonElement<MfpMibAgentDevice>(snapshot.data!!.toJsonObject())
                    offer(ag)
                } else close()
            }
        })
        awaitClose { listener.remove() }
    }.flatMapLatest { agent -> // クエリをチェックしあればflow
        val queryCollectionPath = agent.dev.confidPath
                ?: firestore.collection("device").document(agentId).collection("query").path
        println("Q=$queryCollectionPath") //TODO
        callbackFlow<Work> {
            val listener = firestore.collection(queryCollectionPath).addSnapshotListener(object : EventListener<QuerySnapshot?> {
                override fun onEvent(value: QuerySnapshot?, error: FirestoreException?) {
                    if (error == null && value != null) for (e in value.documents) {
                        val query = Json { ignoreUnknownKeys = true }.decodeFromJsonElement<MfpMibAgentQuery>(e.data.toJsonObject())
                        offer(Work(agent, query))
                        println("query:$query")
                    } else close()
                }
            })
            awaitClose { listener.remove() }
        }
    }.flatMapLatest { params -> // スケジュール時間になれば、次に進む
        channelFlow {
            params.query.schedule.limit.let {
                repeat(it) {
                    offer(params)
                    delay(params.query.schedule.interval)
                }
            }
        }
    }.flatMapLatest { params -> // SNMP検索し、
        val devSet = mutableSetOf<String>()
        channelFlow<DeviceInfo> {
            launch {
                params.query.scanAddrSpecs.forEach { target ->
                    discoveryDeviceFlow(target, snmp).collect { ev -> // 発見デバイス情報を次に流す
                        val res = ResponseEvent(reqPdu = PDU.from(ev.request), reqTarget = target, resPdu = PDU.from(ev.response),
                                resTarget = SnmpTarget(
                                        addr = ev.peerAddress.inetAddress.hostAddress, port = ev.peerAddress.port,
                                        credential = target.credential, retries = target.retries, interval = target.interval))
                        println(res.resTarget.addr)
                        val devId = "type=mfp.mib:model=${res.resPdu.vbl[0].value}:sn=${res.resPdu.vbl[1].value}"
                        if (params.query.autoRegistration) { // 自己デバイス登録
                            firestore.collection("device").document(devId).set(MfpMibDevice(
                                    dev = GdvmDeviceInfo(
                                            cluster = params.agent.dev.cluster, // Agentと同じものを登録
                                            password = params.agent.dev.password, // (デフォルトPWのほうがよいだろうか?)
                                    )
                            ))
                        }
                        devSet.add(devId)
                        offer(DeviceInfo(devId, params.agent.dev.password, res.resTarget))
                    }
                }
            }.join()
            // 検索後、結果をレポート
            val rep = Report(
                    time = Date().time,
                    deviceId = agentId,
                    result = Result(detected = devSet.toList()),
            )
            firestore.collection("device").document(agentId).collection("state").document("discovery").set(rep)
            firestore.collection("device").document(agentId).collection("logs").document().set(rep)
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
                InetAddress.getByName(target.addrRangeEnd ?: target.addr) //TODO:BLocking code
        ).collect {
            offer(it)
        }
    }
}
