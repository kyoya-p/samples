import firebaseInterOp.Firebase
import firebaseInterOp.Firebase.User
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import netSnmp.Snmp

val snmpOptions = mapOf(
    "port" to 161,
    "retries" to 5,
    "timeout" to 5000,
    "version" to 0,
)

external val process: dynamic
val args: Array<String> get() = process.argv

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

val firebase = Firebase.initializeApp(
    apiKey = "AIzaSyDrO7W7Sb6RCpHTsY3GaP-zODRP_HtY4nI",
    authDomain = "road-to-iot.firebaseapp.com",
    projectId = "road-to-iot"
)

@ExperimentalCoroutinesApi
suspend fun runSnmpAgent(agentId: String) {
    val docDev = firebase.firestore.collection("device").document(agentId)
    callbackFlow { docDev.get { offer(it) };awaitClose() }.collectLatest { docSnapshot ->
        println(docSnapshot.data)
    }
}

// debug
fun String.toB64() = chunked(4).map {
    it.map { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".indexOf(it) }
        .foldIndexed(0) { i, a, e -> a or (e shl (18 - 6 * i)) }
}.flatMap { (0..2).map { i -> (it shr (16 - 8 * i) and 255).toChar() } }.joinToString("")