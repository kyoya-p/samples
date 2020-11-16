package gdvm.agent.mib

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.EventListener
import com.google.cloud.firestore.FirestoreException
import firestoreInterOp.firestoreDocumentFlow
import mibtool.*
import com.google.cloud.firestore.FirestoreOptions
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
        val agentDeviceId = if (args.isEmpty()) "agent1" else args[0]
        println("Start Agent ${agentDeviceId}")
        runAgent(agentDeviceId)
        println("Terminated Agent ${agentDeviceId}")
    }.onFailure { it.printStackTrace() }
}


@ExperimentalCoroutinesApi
suspend fun runAgent(agentId: String) = coroutineScope {
    data class DeviceInfo(val id: String, val password: String, val target: SnmpTarget) //TODO:

    callbackFlow<DocumentSnapshot> {  // Firestoreから設定を読めれば(または更新されたら)、内容を流す
        val listener = firestore.collection("device").document(agentId).addSnapshotListener(object : EventListener<DocumentSnapshot?> {
            override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) {
                if (ex == null && snapshot != null && snapshot.exists() && snapshot.data != null) offer(snapshot)
                else close()
            }
        })
        awaitClose { listener.remove() }
    }.mapLatest { it -> // Jsonを介して構造体に格納
        Json { ignoreUnknownKeys = true }.decodeFromJsonElement<MfpMibAgentDevice>(it.data!!.toJsonObject())
    }.flatMapLatest { docAg -> // スケジュール時間になれば、次に進む
        channelFlow { docAg.config?.schedule?.limit?.let { repeat(it) { offer(docAg);delay(docAg.config.schedule.interval) } } }
    }.flatMapLatest { docAg -> // SNMP検索し、
        val devSet = mutableSetOf<String>()
        channelFlow<DeviceInfo> {
            val config = docAg.config!!
            launch {
                config.scanAddrSpecs.forEach { target ->
                    discoveryDeviceFlow(target, snmp).collect { ev -> // 発見デバイス情報を次に流す
                        val res = ResponseEvent(reqPdu = PDU.from(ev.request), reqTarget = target, resPdu = PDU.from(ev.response),
                                resTarget = SnmpTarget(
                                        addr = ev.peerAddress.inetAddress.hostAddress, port = ev.peerAddress.port,
                                        credential = target.credential, retries = target.retries, interval = target.interval))
                        println(res.resTarget.addr)
                        val devId = "type=mfp.mib:model=${res.resPdu.vbl[0].value}:sn=${res.resPdu.vbl[1].value}"
                        if (config.autoRegistration) { // 自己デバイス登録
                            firestore.collection("device").document(devId).set(MfpMibDevice(
                                    cluster = docAg.cluster, // Agentと同じものを登録
                                    password = docAg.password, // (デフォルトPWのほうがよいだろうか?)
                                    info = MfpMibDeviceInfo(
                                            model = res.resPdu.vbl[0].value,
                                            sn = res.resPdu.vbl[1].value,
                                    )
                            ))
                        }
                        devSet.add(devId)
                        offer(DeviceInfo(devId, docAg.password, res.resTarget))
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


@ExperimentalCoroutinesApi
suspend fun runAgent2(agentId: String) = coroutineScope {
    firestore.firestoreDocumentFlow<MfpMibAgentDevice> { collection("device").document(agentId) }
            .map { println(it.config);it.config!! }
            .snmpAgentscheduledFlow()
            .collectLatest { req ->
                val devSet = mutableSetOf<String>()
                val j = launch {
                    // 検索とProxyデバイスの生成
                    req.scanAddrSpecs.forEach { target ->
                        discoveryDeviceFlow(target, snmp).collect { ev ->
                            val res = ResponseEvent(reqPdu = PDU.from(ev.request), reqTarget = target, resPdu = PDU.from(ev.response),
                                    resTarget = SnmpTarget(
                                            addr = ev.peerAddress.syntaxString, port = ev.peerAddress.port,
                                            credential = target.credential, retries = target.retries, interval = target.interval))
                            val devId = "type=mfp.mib:model=${res.resPdu.vbl[0].value}:sn=${res.resPdu.vbl[1].value}"
                            if (!devSet.contains(devId)) {
                                devSet.add(devId)
                                launch {
                                    runMfp(devId, "Sharp-#1", res.resTarget)
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
                    if (req.autoRegistration) {                    // TODO

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
suspend fun Flow<MfpMibAgentConfig>.snmpAgentscheduledFlow() = channelFlow {
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
