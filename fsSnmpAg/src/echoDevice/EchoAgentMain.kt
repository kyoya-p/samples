package echoDevice

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.EventListener
import com.google.cloud.firestore.FirestoreException
import firestoreInterOp.toJsonObject
import gdvm.agent.mib.GdvmDeviceInfo
import gdvm.agent.mib.GdvmMessageInfo
import gdvm.agent.mib.firestore
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement


@Serializable
data class EchoAgent(
        val dev: GdvmDeviceInfo,
        val type: Map<String, Map<String, Map<String, Map<String, String>>>> = mapOf("dev" to mapOf("agent" to mapOf("echo" to mapOf()))),
        val devList: List<String>,
)

@Serializable
data class EchoDevice(
        val dev: GdvmDeviceInfo,
        val type: Map<String, Map<String, Map<String, String>>> = mapOf("dev" to mapOf("echo" to mapOf())),
)

@Serializable
data class BroadcastMessage(
        val msg: GdvmMessageInfo,
)

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
fun main(args: Array<String>): Unit = runBlocking {
    runCatching {
        val agentDeviceIds = if (args.isEmpty()) arrayOf("echo1") else args
        for (agentDeviceId in agentDeviceIds) {
            launch {
                println("Start Echo Agent ${agentDeviceId}")
                runEchoAgent(agentDeviceId)
                println("Terminated Echo Agent ${agentDeviceId}")
            }
        }
    }.onFailure { it.printStackTrace() }
}

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
suspend fun runEchoAgent(agentId: String) = coroutineScope {
    callbackFlow<EchoAgent> {  // Agent情報をチェックし変更あればflow
        val listener = firestore.collection("device").document(agentId)
                .addSnapshotListener(object : EventListener<DocumentSnapshot?> {
                    override fun onEvent(value: DocumentSnapshot?, error: FirestoreException?) {
                        if (error == null && value != null && value.exists() && value.data != null) {
                            println(value.data)
                            try {
                                val ag = Json { ignoreUnknownKeys = true }
                                        .decodeFromJsonElement<EchoAgent>(value.data!!.toJsonObject())
                                offer(ag)
                            } catch (e: Exception) {
                                println(e)
                            }
                        } else close()
                    }
                })
        awaitClose { listener.remove() }
    }.collectLatest { echoAgent ->        // Agentが勝手にデバイス登録し、各echoDeviceを起動
        echoAgent.devList.forEach { devId ->
            val echoDev = EchoDevice(dev = GdvmDeviceInfo(cluster = echoAgent.dev.cluster, name = devId))
            firestore.collection("device").document(devId).set(echoDev)
            launch {
                runEchoDevice(devId)
            }
        }
    }
}

@InternalCoroutinesApi
suspend fun runEchoDevice(deviceId: String) = coroutineScope {
    callbackFlow<EchoDevice> {  // FirestoreからDevice情報を取得
        val listener = firestore.collection("device").document(deviceId)
                .addSnapshotListener(object : EventListener<DocumentSnapshot?> {
                    override fun onEvent(value: DocumentSnapshot?, error: FirestoreException?) {
                        if (error == null && value != null && value.exists() && value.data != null) {
                            try {
                                val echoDevice = Json { ignoreUnknownKeys = true }
                                        .decodeFromJsonElement<EchoDevice>(value.data!!.toJsonObject())
                                offer(echoDevice)
                            } catch (e: Exception) {
                                println(e)
                            }
                        }// else close()
                    }
                })
        awaitClose { listener.remove() }
    }.collectLatest { echoDevice ->
        // 所属Group全てのqueryを参照
        generateSequence(echoDevice.dev.cluster) { grId ->
            firestore.collection("group").document(grId).get().get()["parent"] as String?
        }.forEach { ancestor ->
            launch {
                println("Device[${echoDevice.dev.name}] listening broadcast of Group[$ancestor] ")
                callbackFlow<BroadcastMessage> {
                    val listener = firestore.collection("group").document(ancestor).collection("channel").document("broadcast")
                            .addSnapshotListener(object : EventListener<DocumentSnapshot?> {
                                override fun onEvent(value: DocumentSnapshot?, error: FirestoreException?) {
                                    if (error == null && value != null && value.exists() && value.data != null) {
                                        try {
                                            val echoDevice = Json { ignoreUnknownKeys = true }
                                                    .decodeFromJsonElement<BroadcastMessage>(value.data!!.toJsonObject())
                                            offer(echoDevice)
                                        } catch (e: Exception) {
                                            println(e)
                                        }
                                    }
                                }
                            })
                    awaitClose { listener.remove() }
                }.collect {
                    // Broadcast処理
                    println(it)
                }
            }
        }
        println("listening query")
    }
}
