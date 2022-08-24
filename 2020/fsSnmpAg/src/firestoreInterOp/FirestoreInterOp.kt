package firestoreInterOp

import com.google.cloud.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*

inline fun Firestore.firestoreEventFlow(noinline query: Firestore.() -> DocumentReference) = channelFlow<DocumentSnapshot> {
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




