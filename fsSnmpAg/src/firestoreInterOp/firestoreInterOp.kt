package firestoreInterOp

import MfpMibAgent.AgentRequest
import MfpMibAgent.ScanAddrSpec
import com.google.cloud.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*
import mibtool.Credential
import mibtool.SnmpTarget
import java.io.Serializable


inline fun <R> firestoreScopeDefault(op: (Firestore) -> R): R = op(FirestoreOptions.getDefaultInstance().getService()!!)

inline fun Firestore.firestoreEventFlow(noinline query: Firestore.() -> DocumentReference) = channelFlow<DocumentSnapshot> {
//    val db = FirestoreOptions.getDefaultInstance().getService()!!
    val listener = query().addSnapshotListener(object : EventListener<DocumentSnapshot?> {
        override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) { // ドキュメントRead/Update時ハンドラ
            if (ex == null && snapshot != null && snapshot.exists() && snapshot.data != null) offer(snapshot)
            else close()
        }
    })
    awaitClose { listener.remove() }
}


inline fun <reified T : Any> Firestore.firestoreDocumentFlow(noinline query: Firestore.() -> DocumentReference) = firestoreEventFlow(query).map { snapshot ->
    Json { ignoreUnknownKeys = true }.decodeFromJsonElement<T>(snapshot.data!!.toJsonObject())
}

fun Map<String, Any>.toJsonObject(): JsonObject = buildJsonObject {
    forEach { (k, v) ->
        when (v) {
            is Number -> put(k, v)
            is String -> put(k, v)
            is Boolean -> put(k, v)
            is Map<*, *> -> put(k, (v as Map<String, Any>).toJsonObject())
            is List<*> -> put(k, (v as List<Any>).toJsonArray())
        }
    }
}

fun List<Any>.toJsonArray(): JsonArray = buildJsonArray {
    forEach { v ->
        when (v) {
            is Number -> add(v)
            is String -> add(v)
            is Boolean -> add(v)
            is Map<*, *> -> add((v as Map<String, Any>).toJsonObject())
            is List<*> -> add((v as List<Any>).toJsonArray())
        }
    }
}

/*
fun AgentRequest.Companion.from(obj: Map<String?, *>) = AgentRequest(
        scanAddrSpecs = (obj["scanAddrSpecs"] as List<Map<String?, *>>).map { ScanAddrSpec.from(it) },
        //filter = ((obj["filter"] as Map<String, *>)?.let{},
        //report = ((obj["report"] as Map<String, *>)["oids"] as List<String>).map { it },
        //snmpConfig = if (obj["snmpConfig"] == null) SnmpConfig() else SnmpConfig.from(obj["snmpConfig"] as Map<String, *>)
        autoDetectedRegister = (obj["autoDetectedRegister"] as Boolean?) ?: false
)
*/

/*
fun ScanAddrSpec.Companion.from(obj: Map<String?, *>) = ScanAddrSpec(
        broadcastAddr = obj["broadcastAddr"] as String?,
        //addr = obj["addr"] as String?,
        addrUntil = obj["addrUntil"] as String?,
        //credential = obj["credential"]?.let { Credential.from(it as Map<String?, Any?>) } ?: Credential(),
        //interval = obj["interval"]?.let { it as Long } ?: 5000L,
        //retries = obj["retries"]?.let { it as Int } ?: 5,
        target = (obj["target"] as Map<String?, *>?)?.let { SnmpTarget.from(it) } ?: SnmpTarget()
)
*/

/*
fun SnmpTarget.Companion.from(obj: Map<String?, *>) = SnmpTarget(
        addr = obj["addr"] as String?,
        port = obj["port"] as Int? ?: 161,
        credential = (obj["credential"] as Map<String?, *>?)?.let { Credential.from(it) } ?: Credential(),
        retries = obj["retries"] as Int? ?: 5,
        interval = obj["interval"] as Long? ?: 5_000,
)

fun Credential.Companion.from(obj: Map<String?, *>) = Credential(
        //TODO
)
*/



