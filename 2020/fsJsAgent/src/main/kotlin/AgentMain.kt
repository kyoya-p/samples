package launcherAgent

import firebaseInterOp.*
import firebaseInterOp.Firestore.*
import gdvm.agent.mib.*
import gdvm.agent.stressTestAgent.runStressTestAgent
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import snmpMfpDevice.runSnmpMfpDevice
import kotlin.js.Date

external val process: dynamic
val args: Array<String> get() = process.argv

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

// 認証後、与えられたidに対応するtype情報を取得し、応じたProxyDevice/Agentを起動する
@InternalCoroutinesApi
suspend fun runGenericDevice(deviceId: String, secret: String) = coroutineScope {
    val firebase = Firebase.initializeApp(
        apiKey = "AIzaSyDrO7W7Sb6RCpHTsY3GaP-zODRP_HtY4nI",
        authDomain = "road-to-iot.firebaseapp.com",
        projectId = "road-to-iot",
        name = deviceId,
    )
    try {
        println("Start Device ID:$deviceId    (Ctrl-C to Terminate)")
        mainGenericDevice(firebase, deviceId, secret)
    } finally {
        withContext(NonCancellable) {
            val n = firebase.name
            firebase.delete().await()
            println("Released Firestore app: $n")
        }
    }
}

@InternalCoroutinesApi
suspend fun mainGenericDevice(firebase: App, deviceId: String, secret: String) = coroutineScope {

    // Login Flow
    // CustomTokenは一時間で切れるので、再ログイン(Token再発行)が必要
    callbackFlow {
        val urlQuery = listOf("id" to deviceId, "pw" to secret).formUrlEncode()
        val urlCustomToken = "$customTokenSvr/customToken?$urlQuery"
        println("Request Custom Token: $urlCustomToken")
        val customToken = HttpClient().get<String>(urlCustomToken)
        println("Custom Token Claim ${customToken.claim()}")
        if (customToken.isEmpty()) {
            println("Custom Authentication Error")
            return@callbackFlow
        }
        firebase.auth().signInWithCustomToken(customToken)
        firebase.auth().onAuthStateChanged { if (it != null) offer(it) }
        awaitClose()
    }.flatMapLatest {
        println("Signed-in: $deviceId  ${Date().toTimeString()}")
        // デバイス対応識別情報を取得
        channelFlow {
            val db = firebase.firestore()
            val dev = db.collection("device").document(deviceId).get("type").await().data
            dev?.get("type")?.let { offer(it) }
            awaitClose()
        }
    }.collectLatest { type ->
        println("Device Type: ${type}")

        operator fun JsonElement?.get(key: String): JsonElement? = if (this is JsonObject) this[key] else null
        when {
            type["dev"]["mfp"]["snmp"] != null -> runSnmpMfpDevice(firebase, deviceId, secret)
            //type["dev"]["agent"]["snmp"] != null -> runSnmpAgent(firebase, deviceId, secret)
            type["dev"]["agent"]["stressTest"] != null -> runStressTestAgent(firebase, deviceId, secret)
            type["dev"]["agent"]["launcher"] != null -> runLauncherAgent(firebase, deviceId, secret)
        }
    }
}

// debug
fun String.claim() = split(".").drop(1).first().fromBase64()
fun String.fromBase64() = chunked(4).map {
    it.map { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".indexOf(it) }
        .foldIndexed(0) { i, a, e -> a or (e shl (18 - 6 * i)) }
}.flatMap { (0..2).map { i -> (it shr (16 - 8 * i) and 255).toChar() } }.joinToString("")
