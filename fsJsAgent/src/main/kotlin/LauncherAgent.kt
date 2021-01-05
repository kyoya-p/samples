package launcherAgent

import firebaseInterOp.*
import gdvm.agent.mib.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable // kotlin/js (1.4)では暗黙のserializerは実行時エラーになるようだ
data class AgentLauncher(
    val dev: GdvmDeviceInfo,
    val type: JsonObject, // "type":{"dev":{"agent":{"launcher":{}}}}
    val targets: List<TargetInfo>,
) {
    companion object
}

fun AgentLauncher.Companion.fromJson(j: JsonObject) =
    AgentLauncher(
        dev = GdvmDeviceInfo.fromJson(j["dev"]!!.toJsonObject()),
        type = j["type"]!!.toJsonObject(),
        targets = j["targets"]!!.toJsonArray()
            .map { TargetInfo.fromJson(it.toJsonObject()) }
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

@InternalCoroutinesApi
suspend fun runLauncherAgent(firebase: App, deviceId: String, secret: String) {
    println("Start LauncherAgent. id:$deviceId")

    val db = firebase.firestore()

    callbackFlow {
        val d = db.collection("device").document(deviceId).addSnapshotListener { devSS ->
            devSS?.data?.let {
                val s = it.toString()
                println("s: $s")
                println("sje: ${s.toJsonElement()}")
                println("itje: ${it.toJsonElement()}")
                val d: AgentLauncher = Json { ignoreUnknownKeys = true }.decodeFromString(s)
                println("d:$d")
                offer(d)
            }
        }
        awaitClose()
    }.collectLatest { agLauncher ->
        println(agLauncher.targets) //TODO
        agLauncher.targets.forEach { target ->
            runGenericDevice(target.id, target.password)
        }
    }
}

