import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.js.json

@Serializable
data class DeviceDocument(
    val id: String,
    val type: String,
)

@Serializable
data class LoadAgentDocument(
    val dev: DeviceDocument,
    val config: LoadDeviceConfig = LoadDeviceConfig(),
)

@Serializable
data class LoadDeviceConfig(
    val instance: Int = 1,
    val interval: Int = 10000,
    val repeat: Int = 1,
)

val db = firebase.firestore

fun runLoadAgent(agentId: String) = channelFlow {
    db.collection("device").doc(agentId).get {
        val devDoc: LoadAgentDocument =
            Json { ignoreUnknownKeys = true }.decodeFromString(JSON.stringify(it.data))
        println("devDoc=${devDoc}")
        repeat(devDoc.config.instance) {
            //val v = runSubAgent(devDoc, it)
            launch { offer(devDoc to it) }
        }
    }
    awaitClose { }
}

suspend fun runSubAgent(devDoc: LoadAgentDocument, number: Int) {
    println("Started SubAgent($number)")
    delay(1000)
    val vm = json(
        "cluster" to "#example",
        "data" to json(
            "message" to "Hello World"
        )
    )

    db.collection("device").doc(devDoc.dev.id).collection("logs").doc().set(vm) {}
    println("Completed SubAgent($number)")
}
