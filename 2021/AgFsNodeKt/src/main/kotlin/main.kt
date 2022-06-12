import NodeJS.set
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import dev.gitlive.firebase.initialize
import gdvm.agent.mib.GdvmGenericDevice
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlin.js.Date

@ExperimentalCoroutinesApi
suspend fun main() {
    try {
        println("Start client")

        //process.env["NODE_TLS_REJECT_UNAUTHORIZED"] = "0" // TLS証明書チェックをバイパス
        //process.env["GLOBAL_AGENT_HTTP_PROXY"] = "http://10.144.98.32:3080/"
        require("global-agent/bootstrap")

        val opts = FirebaseOptions(
            applicationId = "1:307495712434:web:acc483c0c300549ff33bab",
            apiKey = "AIzaSyDrO7W7Sb6RCpHTsY3GaP-zODRP_HtY4nI",
            databaseUrl = "https://road-to-iot.firebaseio.com",
            projectId = "road-to-iot",
        )
        Firebase.initialize(context = null, options = opts)

        println("Initialized Database")
        mainGenericDevice("Display", "1234eeee") //TODO
        println("Terminated clint")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@ExperimentalCoroutinesApi
suspend fun mainGenericDevice(deviceId: String, secret: String) = coroutineScope {
    val db = Firebase.firestore
    // CustomTokenは一時間で切れるので、再ログイン(Token再発行)が必要
    siginInWithCustomToken(deviceId, secret).flatMapLatest { // ログイン後、デバイス情報取得
        println("Signed-in: $deviceId ${Date().toTimeString()}")
        callbackFlow {
            db.collection("device").document(deviceId).snapshots.collect { offer(it.data<GdvmGenericDevice>()) }
            awaitClose()
        }

    }.collectLatest { dev ->
        println("Device: ${dev}")
        when {
            //dev.type.contains("dev.mfp.snmp") -> println(dev)
            dev.type.contains("dev.idp.sc") -> runIdpDevice(dev)
            //dev.type.contains("dev.agent.snmp") -> println(dev)
            //dev.type.contains("dev.agent.idp.sc") -> runIdpScAgent(dev)
        }
    }
}

@ExperimentalCoroutinesApi
suspend fun runIdpDevice(dev: GdvmGenericDevice) {
    val db = Firebase.firestore
    db.collection("device").document(dev.id).collection("query")
        .where("cluster", equalTo = dev.cluster).snapshots.flatMapLatest {
            channelFlow {
                it.documents.forEach {
                    println("Data: ${it.data<String>()}")
                    offer(it.data<IdpScDeviceQuery>())
                }
            }
        }.collectLatest {
            println(it)
        }
}

suspend fun siginInWithCustomToken( deviceId: String, secret: String): Flow<FirebaseUser?> = run {
    val customTokenSvr = "https://us-central1-road-to-iot.cloudfunctions.net/requestToken"
    val urlQuery = listOf("id" to deviceId, "pw" to secret).formUrlEncode()
    val urlCustomToken = "$customTokenSvr/customToken?$urlQuery"
    println("Request Custom Token: $urlCustomToken")
    val customToken = HttpClient().get<String>(urlCustomToken)
    println("Custom Token Claim ${customToken.claim()}") //TODO debug

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

