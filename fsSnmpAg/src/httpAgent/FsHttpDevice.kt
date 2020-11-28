package httpAgent

import com.google.cloud.firestore.*
import firestoreInterOp.toJsonObject
import gdvm.agent.mib.Schedule
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.response.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.io.use

@Serializable
data class SharpHttpDeviceDocument(
        val config: SharpHttpDeviceConfig
)

@Serializable
data class SharpHttpDeviceConfig(
        val schedule: Schedule = Schedule(1),
        val httpRequest: HttpProxyRequest,
)

@Serializable
data class HttpProxyRequest(
        val url: String,
        val method: String,
        val headers: Map<String, String>,
        val body: String,
)

@Serializable
data class HttpProxyResponse(
        val status: Int,
        val headers: Map<String, String>,
        val body: String,
)

val firestore = FirestoreOptions.getDefaultInstance().getService()!!

fun main(args: Array<String>) {
    val agentId = if (args.size == 0) "httpDevice1" else args[0]

    callbackFlow<DocumentSnapshot> {  // Firestoreから設定を読めれば(または更新されたら)、内容を流す
        val listener = gdvm.agent.mib.firestore.collection("device").document(agentId).addSnapshotListener(object : EventListener<DocumentSnapshot?> {
            override fun onEvent(snapshot: DocumentSnapshot?, ex: FirestoreException?) {
                if (ex == null && snapshot != null && snapshot.exists() && snapshot.data != null) offer(snapshot)
                else close()
            }
        })
        awaitClose { listener.remove() }
    }.mapLatest { it -> // Jsonを介して構造体に変換して流す
        Json { ignoreUnknownKeys = true }.decodeFromJsonElement<SharpHttpDeviceDocument>(it.data!!.toJsonObject())
    }.mapLatest {
        it    //TODO:スケジュール
    }.mapLatest { httpDevDoc -> //HTTPリクエストを処理し結果を流す
        HttpClient().use { client ->
            client.request<HttpStatement> {
                method = HttpMethod(httpDevDoc.config.httpRequest.method)
                url(httpDevDoc.config.httpRequest.url)
                headers { httpDevDoc.config.httpRequest.headers }
                body = httpDevDoc.config.httpRequest.body
            }
        }
    }.mapLatest { httpRes ->
       /* HttpProxyResponse(
                status = httpRes.,
                headers = httpRes.headers
                val body : String,
        )

        */
    }
}
