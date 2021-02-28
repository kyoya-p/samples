import kotlinx.browser.document

import dev.gitlive.firebase.*
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import gdvm.agent.mib.GdvmGenericDevice
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlin.js.Date


val firebase: FirebaseApp by lazy {
    val opts = FirebaseOptions(
        applicationId = "1:307495712434:web:acc483c0c300549ff33bab",
        apiKey = "AIzaSyDrO7W7Sb6RCpHTsY3GaP-zODRP_HtY4nI",
        databaseUrl = "https://road-to-iot.firebaseio.com",
        projectId = "road-to-iot",
    )
    Firebase.initialize(context = null, options = opts)
}

suspend fun main() {
    val fb = firebase
    mainGenericDevice("PSDD", "1234eeee") //TODO
}

suspend fun mainGenericDevice(deviceId: String, secret: String) = coroutineScope {

    // Login Flow
    // CustomTokenは一時間で切れるので、再ログイン(Token再発行)が必要
    siginInWithCustomToken(deviceId, secret).flatMapLatest {
        println("Signed-in: $deviceId  ${Date().toTimeString()}")
        // デバイスタイプ取得
        channelFlow {
            val db = Firebase.firestore
            val dev = db.collection("device").document(deviceId).get().data<GdvmGenericDevice>()
            offer(dev)
            awaitClose()
        }
    }.collectLatest { dev ->
        println("Device: ${dev}")
        document.write("$dev")
        when {
            dev.type.contains("dev.mfp.snmp") -> println(dev)
            //type["dev"]["agent"]["snmp"] != null -> runSnmpAgent(firebase, deviceId, secret)
            //type["dev"]["agent"]["stressTest"] != null -> runStressTestAgent(firebase, deviceId, secret)
            //type["dev"]["agent"]["launcher"] != null -> runLauncherAgent(firebase, deviceId, secret)
        }
    }
}

suspend fun siginInWithCustomToken(deviceId: String, secret: String): Flow<FirebaseUser?> = run {
    val customTokenSvr = "https://us-central1-road-to-iot.cloudfunctions.net/requestToken"
    val urlQuery = listOf("id" to deviceId, "pw" to secret).formUrlEncode()
    val urlCustomToken = "$customTokenSvr/customToken?$urlQuery"
    println("Request Custom Token: $urlCustomToken")
    val customToken = HttpClient().get<String>(urlCustomToken)
    println("Custom Token Claim ${customToken.claim()}")
    if (customToken.isEmpty()) {
        println("Custom Authentication Error")
    }
    val auth = Firebase.auth
    auth.signInWithCustomToken(customToken)
    return auth.authStateChanged
}

// debug
fun String.claim() = split(".").drop(1).first().fromBase64()
fun String.fromBase64() = chunked(4).map {
    it.map { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".indexOf(it) }
        .foldIndexed(0) { i, a, e -> a or (e shl (18 - 6 * i)) }
}.flatMap { (0..2).map { i -> (it shr (16 - 8 * i) and 255).toChar() } }.joinToString("")
