package launcherAgent

import SnmpAgent
import firebaseInterOp.Firebase
import firebaseInterOp.Firestore.*
import firebaseInterOp.decodeFrom
import firebaseInterOp.toJsonArray
import firebaseInterOp.toJsonObject
import gdvm.agent.mib.*
import gdvm.agent.stressTestAgent.runStressTestAgent
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import runSnmpMfpDevice

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
    val deviceId = args[2]
    val secret = args[3]
    runGenericDevice(deviceId, secret)
}.join()

@Serializable // kotlin/js (1.4)では暗黙のserializerは実行時エラーになるようだ
data class MainAgentLauncher(
    val dev: GdvmDeviceInfo,
    val type: JsonObject, // "type":{"dev":{"agent":{"launcher":{}}}}
    val targets: List<TargetInfo>,
) {
    companion object
}

fun MainAgentLauncher.Companion.fromJson(j: JsonObject) =
    MainAgentLauncher(
        dev = GdvmDeviceInfo.fromJson(j["dev"]!!.toJsonObject()),
        type = j["type"]!!.toJsonObject(),
        targets = j["targets"]!!.toJsonArray()
            .map { println("fromJson: $it -> id=${it.toJsonObject()["id"]}"); TargetInfo.fromJson(it.toJsonObject()) }
    )


@Serializable
data class TargetInfo(
    val id: String,
    val password: String,
) {
    companion object
}

fun TargetInfo.Companion.fromJson(j: JsonObject) = TargetInfo(
    id = (j["id"] as JsonPrimitive).content,
    password = (j["password"] as JsonPrimitive).content,
)

// 認証後、与えられたidに対応するtype情報を取得し、応じたProxyDevice/Agentを起動する
@InternalCoroutinesApi
suspend fun runGenericDevice(deviceId: String, secret: String) = coroutineScope {
    println("Start Device ID:$deviceId    (Ctrl-C to Terminate)")

    val firebase = Firebase.initializeApp(
        apiKey = "AIzaSyDrO7W7Sb6RCpHTsY3GaP-zODRP_HtY4nI",
        authDomain = "road-to-iot.firebaseapp.com",
        projectId = "road-to-iot",
        name = deviceId,
    )

    // Login Flow
    // CustomTokenは一時間で切れるので、再ログイン(Token再発行)が必要
    callbackFlow {
        val customToken = HttpClient().get<String>("$customTokenSvr/customToken?id=$deviceId&pw=$secret")
        if (customToken.isEmpty()) {
            println("Custom Authentication Error")
            return@callbackFlow
        }
        firebase.auth().signInWithCustomToken(customToken)
        firebase.auth().onAuthStateChanged { if (it != null) offer(it) }
        awaitClose()
    }.flatMapLatest {
        println("Signed-in: $deviceId")
        // デバイス対応識別情報を取得
        channelFlow {
            val db = firebase.firestore()
            val dev = db.collection("device").document(deviceId).get().await().data
            dev?.get("type")?.let { offer(it) }
            awaitClose()
        }
    }.collectLatest { type ->
        println("Device Type: ${type}")

        operator fun JsonElement?.get(key: String): JsonElement? = if (this is JsonObject) this[key] else null
        when {
            type["dev"]["mfp"]["snmp"] != null -> runSnmpMfpDevice(firebase, deviceId, secret)
            type["dev"]["agent"]["stressTest"] != null -> runStressTestAgent(firebase, deviceId, secret)
            type["dev"]["agent"]["launcher"] != null -> runLauncherAgent(firebase, deviceId, secret)
        }
    }
}


suspend fun runLauncherAgent(firebase: Firebase, deviceId: String, secret: String) {
    println("Start runLauncherAgent. id:$deviceId")

    val db = firebase.firestore()

    callbackFlow {
        val d = db.collection("device").document(deviceId).addSnapshotListener { devSS ->
            devSS?.data?.let { offer(decodeFrom<MainAgentLauncher>(it)) }
        }
        awaitClose()
    }.collectLatest {agLauncher->
        println(agLauncher) //TODO
    }
}


// debug
fun String.fromBase64() = chunked(4).map {
    it.map { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".indexOf(it) }
        .foldIndexed(0) { i, a, e -> a or (e shl (18 - 6 * i)) }
}.flatMap { (0..2).map { i -> (it shr (16 - 8 * i) and 255).toChar() } }.joinToString("")
