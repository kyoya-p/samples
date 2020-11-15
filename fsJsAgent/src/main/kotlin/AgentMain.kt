import firebaseInterOp.Firebase
import firebaseInterOp.Firestore
import firebaseInterOp.Firestore.DocumentSnapshot
import gdvm.agent.mib.*
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import netSnmp.Snmp
import kotlinx.serialization.json.Json
import kotlin.js.Date
import kotlin.math.min
import kotlin.time.milliseconds


val snmpOptions = mapOf(
    "port" to 161,
    "retries" to 5,
    "timeout" to 5000,
    "version" to 0,
)

external val process: dynamic
val args: Array<String> get() = process.argv


val firebase = Firebase.initializeApp(
    apiKey = "AIzaSyDrO7W7Sb6RCpHTsY3GaP-zODRP_HtY4nI",
    authDomain = "road-to-iot.firebaseapp.com",
    projectId = "road-to-iot"
)

@ExperimentalCoroutinesApi
suspend fun main(): Unit = GlobalScope.launch {

    //TODO: テスト
    Snmp.createSession("192.168.3.19", "public").getNext(arrayOf("1.3.6")) { error, varbinds ->
        if (error == null) {
            varbinds.forEach { println("${it.oid} = ${it.value}") }
        } else {
            println("Error: ${error}")
        }
    }

    if (args.size != 5) {
        println("syntax: node FsJsAgent.js <customTokernSvr> <agentId> <secret>")
        return@launch
    }
    val customTokenSvr = args[2]
    val agentId = args[3]
    val secret = args[4]

    println("Start SampleAgent/NodeJS. agentId:${agentId}  (Ctrl-C to Terminate)")

    val customToken = HttpClient().get<String>("$customTokenSvr/customToken?id=$agentId&pw=$secret")
    if (customToken.isEmpty()) {
        println("Custom Authentication Error")
        return@launch
    }
    customToken.split(".").take(2).forEach { println(it.toB64()) } //TODO: debug

    firebase.auth.signInWithCustomToken(customToken)
    callbackFlow { firebase.auth.onAuthStateChanged { offer(it) };awaitClose() }.collectLatest { user ->
        if (user != null) runSnmpAgent(agentId)
    }
}.join()


@ExperimentalCoroutinesApi
suspend fun runSnmpAgent(agentId: String) {
    val docDev = firebase.firestore.collection("device").document(agentId)
    callbackFlow { docDev.get { offer(it) }; awaitClose() }.collectLatest { docSnapshot ->
        val docDevJson = JSON.stringify(docSnapshot.data)
        val docSnmpAg: SnmpAgentDevice = Json { ignoreUnknownKeys = true }.decodeFromString(docDevJson)
        agentMain(docSnmpAg)
    }
}

suspend fun agentMain(docSnmpAgent: SnmpAgentDevice) {
    println("Start agentMain()")
    val config = docSnmpAgent.config!!
    flow {
        repeat(config.schedule.limit) {
            emit(docSnmpAgent)
            delay(min(config.schedule.interval, 30_000))
        }
    }.collectLatest {
        config.scanAddrSpecs.forEach { target ->
            println("${target}")
            callbackFlow {
                Snmp.createSession(target.addr, "public")
                    .getNext(arrayOf(sysDescr, prtGeneralSerialNumber)) { error, varbinds ->
                        if (error == null) {
                            varbinds.forEach { println("${it.oid} = ${it.value}") }
                            val model = "${varbinds[0].value}"
                            val sn = "${varbinds[1].value}"
                            val agentId = "type=mfp.mib:model=$model:sn=$sn"
                            println(agentId)
                            offer(agentId)
                        } else {
                            // Timeout
                        }
                    }
            }.collectLatest { runAgent2(it, target.addr) }
        }
    }
}

// debug
fun String.toB64() = chunked(4).map {
    it.map { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".indexOf(it) }
        .foldIndexed(0) { i, a, e -> a or (e shl (18 - 6 * i)) }
}.flatMap { (0..2).map { i -> (it shr (16 - 8 * i) and 255).toChar() } }.joinToString("")


@ExperimentalCoroutinesApi
suspend fun runAgent2(agentId: String, ipAddr: String) = coroutineScope {
    val db = firebase.firestore
    callbackFlow<Firestore.DocumentSnapshot> { db.collection("device").document(agentId) }
        .map { docSnapshot ->
            val docDevJson = JSON.stringify(docSnapshot.data)
            Json { ignoreUnknownKeys = true }.decodeFromString<GdvmDevice>(docDevJson)
        }.flatMapLatest { docSnmpDevice ->
            db.config.schedule
        }
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
                    time = Date.now(),
                    deviceId = agentId,
                    result = Result(
                        detected = devSet.toList()
                    ),
                )
                // 検索結果をDBに登録
                db.collection("device").document(agentId).collection("logs").document().set(rep)
                db.collection("device").document(agentId).collection("state").document("discovery").set(rep)
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