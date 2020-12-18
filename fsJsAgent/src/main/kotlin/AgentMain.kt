import gdvm.agent.mib.*
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

external val process: dynamic
val args: Array<String> get() = process.argv

val firebase = Firebase.initializeApp(
    apiKey = "AIzaSyDrO7W7Sb6RCpHTsY3GaP-zODRP_HtY4nI",
    authDomain = "road-to-iot.firebaseapp.com",
    projectId = "road-to-iot"
)
val db = firebase.firestore

val customTokenSvr = "https://us-central1-road-to-iot.cloudfunctions.net/requestToken"


@InternalCoroutinesApi
@ExperimentalCoroutinesApi
suspend fun main(): Unit = GlobalScope.launch {
    if (args.size != 4) {
        println("syntax: node FsJsAgent.js <agentId> <secret>")
        return@launch
    }
    val agentId = args[2]
    val secret = args[3]

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
        firebase.auth.onAuthStateChanged { if (it != null) offer(it) }
        awaitClose()
    }.collectLatest {
        //runStressTestAgent(agentId)
        runMainAgent(agentId)
    }
}.join()

@Serializable
data class MainAgent(
    val dev: GdvmDeviceInfo,
    val type: JsonObject, // "type":{"dev":{"agent":{"launcher":{}}}}
    val targets: Map<String/*deviceId*/, TargetInfo>,
)

@Serializable
data class TargetInfo(
    val password: String,
)

@InternalCoroutinesApi
suspend fun runMainAgent(agentId: String) {
    callbackFlow {
        db.collection("device").doc(agentId).get()
    }.collectLatest { agent ->
        print(agent)

    }
}


// debug
fun String.toB64() = chunked(4).map {
    it.map { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".indexOf(it) }
        .foldIndexed(0) { i, a, e -> a or (e shl (18 - 6 * i)) }
}.flatMap { (0..2).map { i -> (it shr (16 - 8 * i) and 255).toChar() } }.joinToString("")