import firebaseInterOp.Firebase
import gdvm.agent.mib.*
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import netSnmp.Snmp
import kotlin.js.json


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
    customToken.split(".").drop(1).take(1).forEach {
        println("Token=${it.toB64()}")
    } //TODO: debug

    callbackFlow {
        firebase.auth.signInWithCustomToken(customToken)
        firebase.auth.onAuthStateChanged { offer(it) }
        awaitClose()
    }.flatMapLatest {
        runLoadAgent(agentId)
    }.collect { (devDoc, n) ->
        runSubAgent(devDoc, n)
    }

}.join()

/*
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


 */

// debug
fun String.toB64() = chunked(4).map {
    it.map { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".indexOf(it) }
        .foldIndexed(0) { i, a, e -> a or (e shl (18 - 6 * i)) }
}.flatMap { (0..2).map { i -> (it shr (16 - 8 * i) and 255).toChar() } }.joinToString("")