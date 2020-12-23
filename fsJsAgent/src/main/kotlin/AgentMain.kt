package launcherAgent

import firebaseInterOp.Firebase
import firebaseInterOp.Firestore.*
import firebaseInterOp.toJsonObject
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
val db = firebase.firestore()

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
    println("Received Custom Token: $agentId ")

//    customToken.split(".").drop(1).take(1).forEach {
//        println("Token=${it.toB64()}")
//    } //TODO: debug

    callbackFlow {
        firebase.auth().signInWithCustomToken(customToken)
        firebase.auth().onAuthStateChanged { if (it != null) offer(it) }
        awaitClose()
    }.collectLatest {
        println("Signed-in: $agentId")
        runMainAgent(agentId)
    }
}.join()

@Serializable // kotlin/js (1.4)では暗黙のserializerは実行時エラーになるようだ
data class MainAgentLauncher(
    val dev: GdvmDeviceInfo,
    val type: JsonObject, // "type":{"dev":{"agent":{"launcher":{}}}}
    val targets: Map<String/*deviceId*/, TargetInfo>,
) {
    companion object
}

fun MainAgentLauncher.Companion.fromJson(j: JsonObject) =
    MainAgentLauncher(
        dev = GdvmDeviceInfo.fromJson(j["dev"]!!.toJsonObject()),
        type = j["type"]!!.toJsonObject(),
        targets = j["targets"]!!.toJsonObject().mapValues { (k, v) -> TargetInfo.fromJson(v.toJsonObject()) }
    )


@Serializable
data class TargetInfo(
    val id: String,
    val password: String,
) {
    companion object
}

fun TargetInfo.Companion.fromJson(j: JsonObject) = TargetInfo(
    id = j["id"].toString(),
    password = j["password"].toString(),
)

@InternalCoroutinesApi
suspend fun runMainAgent(agentId: String) = coroutineScope {
    println("Start SampleAgent/NodeJS. agentId:${agentId}  (Ctrl-C to Terminate)")

    callbackFlow<MainAgentLauncher> {
        val listenerRegister = db.collection("device").document(agentId)
            .addSnapshotListener { doc ->
                val d = doc?.data ?: return@addSnapshotListener
                offer(MainAgentLauncher.fromJson(d))
            }
        awaitClose { listenerRegister.remove() }
    }.collectLatest { agent ->
        println(agent)
        agent.targets.forEach { (devId, tg) ->
            launch { runSubAgent(devId, tg) }
        }
    }
}

suspend fun runSubAgent(id: String, tg: TargetInfo) {
    println("Start SubAgent. id:$id")

    val app = Firebase.initializeApp(
        apiKey = "AIzaSyDrO7W7Sb6RCpHTsY3GaP-zODRP_HtY4nI",
        authDomain = "road-to-iot.firebaseapp.com",
        projectId = "road-to-iot",
        name = id,
    )

    val auth = app.auth()

    val customToken = HttpClient().get<String>("$customTokenSvr/customToken?id=$id&pw=${tg.password}")
    if (customToken.isEmpty()) return
    auth.signInWithCustomToken(customToken)
    callbackFlow {
        auth.onAuthStateChanged { user ->
            if (user != null) offer(user)
        }
        awaitClose()
    }.collectLatest {
        val db = firebase.firestore()
        db.collection("device").document(id).get().await().data?.let {
            println("$id ${it["type"]}")
        }
        suspend fun runMainAgent(agentId: String) = coroutineScope {
            println("Start SampleAgent/NodeJS. agentId:${agentId}  (Ctrl-C to Terminate)")

            callbackFlow<MainAgentLauncher> {
                val listenerRegister = db.collection("device").document(agentId)
                    .addSnapshotListener { doc ->
                        val d = doc?.data ?: return@addSnapshotListener
                        offer(MainAgentLauncher.fromJson(d))
                    }
                awaitClose { listenerRegister.remove() }
            }.collectLatest { agent ->
                println(agent) //TODO
                agent.targets.forEach {
                    launch { runSubAgent(it) }
                }
            }
        }
    }
}

suspend fun runSubAgent(tg: TargetInfo) {
    println("Start SubAgent. id:${tg.id}")

    val app = Firebase.initializeApp(
        apiKey = "AIzaSyDrO7W7Sb6RCpHTsY3GaP-zODRP_HtY4nI",
        authDomain = "road-to-iot.firebaseapp.com",
        projectId = "road-to-iot",
        name = tg.id,
    )

    val auth = app.auth()

    val customToken = HttpClient().get<String>("$customTokenSvr/customToken?id=${tg.id}&pw=${tg.password}")
    if (customToken.isEmpty()) return
    println("${tg.id} ${tg.password} $customToken") //TODO
    auth.signInWithCustomToken(customToken)
    callbackFlow {
        auth.onAuthStateChanged { user -> if (user != null) offer(user) }
        awaitClose()
    }.collectLatest { user ->
        println("UID=${user.uid}") //TODO
        val db = firebase.firestore()
        db.collection("device").document(tg.id).get().await().data?.let {
            println("${tg.id} ${it["type"]}")
        }
    }
}


// debug
fun String.fromBase64() = chunked(4).map {
    it.map { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".indexOf(it) }
        .foldIndexed(0) { i, a, e -> a or (e shl (18 - 6 * i)) }
}.flatMap { (0..2).map { i -> (it shr (16 - 8 * i) and 255).toChar() } }.joinToString("")
