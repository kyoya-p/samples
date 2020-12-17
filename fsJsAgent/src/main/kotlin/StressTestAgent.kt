package gdvm.agent.loadAgent

import firebase
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

val db = firebase.firestore

suspend fun runStressTestAgent(agentId: String) = GlobalScope.launch {
    println("Start Agent($agentId)")
    val devDoc = db.collection("device").doc(agentId).get().await()
    val devQueryDoc = db.collection("device").doc(agentId).collection("query").doc("startup").get().await()
    val dev: Device = fromJson(devDoc.data())
    val devQuery: DeviceQuery = fromJson(devQueryDoc.data())
    for (i in 0 until devQuery.instance) {
        launch {
            runSubAgent(dev, devQuery, i)
        }
    }
}.join()

suspend fun runSubAgent(dev: Device, query: DeviceQuery, number: Int) {
    println("Start SubAgent($number)")
    repeat(query.repeat) {
        delay(query.interval.toLong())
        val log = json(
            "dev" to dev.dev.toJson(),
            "time" to Date.now(),
            "timeRec" to Date(),
            "seq" to it,
        )
        db.collection("device").doc(dev.dev.id).collection("logs").doc().set(log)
        val d=Date()

        println("${d} ${d.getMilliseconds()} pushed log[$it]")
    }
    println("Completed SubAgent($number)")
}

inline fun <reified T> fromJson(j: Any): T = Json { ignoreUnknownKeys = true }.decodeFromString(JSON.stringify(j))
inline fun <reified T> T.toJson(): Any = JSON.parse(Json { ignoreUnknownKeys = true }.encodeToString(this))

