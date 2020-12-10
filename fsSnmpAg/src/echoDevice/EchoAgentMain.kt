package echoDevice

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.EventListener
import com.google.cloud.firestore.FirestoreException
import firestoreInterOp.toJsonObject
import gdvm.agent.mib.GdvmDeviceInfo
import gdvm.agent.mib.firestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement


@Serializable
data class EchoAgent(
        val dev: GdvmDeviceInfo,
        val type: Map<String, Map<String, Map<String, Map<String, String>>>> = mapOf("dev" to mapOf("agent" to mapOf("echo" to mapOf()))),
        val devList: List<String>,
)

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

@ExperimentalCoroutinesApi
suspend fun runEchoAgent(agentId: String) {
    callbackFlow<EchoAgent> {  // Agent情報をチェックし変更あればflow
        val listener = firestore.collection("device").document(agentId)
                .addSnapshotListener(object : EventListener<DocumentSnapshot?> {
                    override fun onEvent(value: DocumentSnapshot?, error: FirestoreException?) {
                        println(error)
                        if (error == null && value != null && value.exists() && value.data != null) {
                            println(value.data)
                            val ag = Json { ignoreUnknownKeys = true }
                                    .decodeFromJsonElement<EchoAgent>(value.data!!.toJsonObject())
                            println(ag)
                            offer(ag)
                        } //else close()
                    }
                })
        awaitClose { listener.remove() }
    }.collectLatest { echoDevice ->
        println(echoDevice)
    }
}
