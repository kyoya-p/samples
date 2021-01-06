package gdvm.agent.stressTestAgent

import firebaseInterOp.App
import firebaseInterOp.Firestore
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.js.Date
import kotlin.js.json

@Serializable
data class DeviceAttr(
    val id: String,
    val type: String,
    val cluster: String,
) {
    companion object
}

@Serializable
data class Device(
    val dev: DeviceAttr,
) {
    companion object
}

@Serializable
data class DeviceQuery(
    val instance: Int = 1,
    val interval: Int = 10000,
    val repeat: Int = 1,
) {
    companion object
}


suspend fun runStressTestAgent(firebase: App, agentId: String, secret: String) = GlobalScope.launch {
    println("Start Agent($agentId)")
    val db = firebase.firestore()
    val devDoc = db.collection("device").document(agentId).get().await()
    val devQueryDoc = db.collection("device").document(agentId).collection("query").document("startup").get().await()
    val dev: Device = fromJson(devDoc.data)
    val devQuery: DeviceQuery = fromJson(devQueryDoc.data)
    for (i in 0 until devQuery.instance) {
        launch {
            runStressTestSubAgent(db,dev, devQuery, i)
        }
    }
}.join()

suspend fun runStressTestSubAgent(db: Firestore, dev: Device, query: DeviceQuery, number: Int) {
    println("Start SubAgent($number)")
    repeat(query.repeat) {
        delay(query.interval.toLong())
        val log = json(
            "dev" to dev.dev.toJson(),
            "time" to Date.now(),
            "timeRec" to Date(),
            "seq" to it,
        )
        db.collection("device").document(dev.dev.id).collection("logs").document().set(log)
        val d = Date()

        println("${d} ${d.getMilliseconds()} pushed log[$it]")
    }
    println("Completed SubAgent($number)")
}

inline fun <reified R> fromJson(obj: Any?): R = Json { ignoreUnknownKeys = true }.decodeFromString(JSON.stringify(obj))
inline fun <reified R> Any?.fromJson(): R = fromJson(this)
inline fun <reified T> T.toJson(): Any = JSON.parse(Json { ignoreUnknownKeys = true }.encodeToString(this))

