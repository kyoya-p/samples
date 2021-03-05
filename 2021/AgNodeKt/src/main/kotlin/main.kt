import NodeJS.set
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.initialize
import gdvm.agent.mib.GdvmGenericDevice
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.browser.document
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlin.js.Date

suspend fun main() {
    println("start client")
    process.env["NODE_TLS_REJECT_UNAUTHORIZED"] = "0" // TLS証明書チェックをバイパス

    val client = HttpClient()
    val httpRes = client.get<String>("https://kyoya-p.github.io/test/build/web/version.json")
    println(httpRes)


    val opts = FirebaseOptions(
        applicationId = "1:307495712434:web:acc483c0c300549ff33bab",
        apiKey = "AIzaSyDrO7W7Sb6RCpHTsY3GaP-zODRP_HtY4nI",
        databaseUrl = "https://road-to-iot.firebaseio.com",
        projectId = "road-to-iot",
    )
    Firebase.initialize(context = null, options = opts)

    mainGenericDevice("PSDD", "1234eeee") //TODO
}

suspend fun mainGenericDevice(deviceId: String, secret: String) = coroutineScope {
    // CustomTokenは一時間で切れるので、再ログイン(Token再発行)が必要
    val db = Firebase.firestore
    siginInWithCustomToken(deviceId, secret).flatMapLatest { // ログイン後、デバイス情報取得
        println("Signed-in: $deviceId ${Date().toTimeString()}")
        callbackFlow {
            db.collection("device").document(deviceId).snapshots.collect {
                offer(it.data<GdvmGenericDevice>())
            }
            awaitClose()
        }
    }.collectLatest { dev ->
        println("Device: ${dev}")
        document.writeln("$dev")
        when {
            dev.type.contains("dev.mfp.snmp") -> println(dev)
            dev.type.contains("dev.agent.snmp") -> println(dev)
            //dev.type.contains("dev.agent.idp.sc") -> runIdpScAgent(dev)
        }
    }
}

suspend fun siginInWithCustomToken(deviceId: String, secret: String): Flow<FirebaseUser?> = run {
    val customTokenSvr = "https://us-central1-road-to-iot.cloudfunctions.net/requestToken"
    val urlQuery = listOf("id" to deviceId, "pw" to secret).formUrlEncode()
    val urlCustomToken = "$customTokenSvr/customToken?$urlQuery"
    println("Request Custom Token: $urlCustomToken")
    val customToken = HttpClient().get<String>(urlCustomToken)
    println("Custom Token Claim ${customToken.claim()}") //TODO debug
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

