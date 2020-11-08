package gifts

import firestoreInterOp.firestoreDocumentFlow
import gdvm.agent.mib.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.Serializable

@Serializable
data class HttpAgentRequest(
        val schedule: Schedule = Schedule(1),
        val request: FsHttpRequest,
)

@Serializable
data class FsHttpRequest(
        val url: String,
        val method: Method = Method.GET,
        val headers: Map<String, String> = mapOf(),
        val body: String? = "",
) {
    enum class Method(val methodString: String) {
        GET("GET"),
        POST("POST"),
    }
}


@Serializable
data class FsHttpResponse(
        val status: Int,
        val httpVer: String,
        val headers: Map<String, String> = mapOf(),
        val body: String,
)

@InternalCoroutinesApi
fun main(args: Array<String>): Unit = runBlocking {
    val agentDeviceIds = if (args.isEmpty()) listOf("httpAgent1") else args.map { it }
    agentDeviceIds.forEach {
        launch {
            println("Start Agent ${agentDeviceIds}")
            runHttpAgent(it)
            println("Terminated Agent ${agentDeviceIds}")
        }
    }
}

val httpClient = HttpClient()

@InternalCoroutinesApi
suspend fun runHttpAgent(agentId: String) {
    do {
        firestore.firestoreDocumentFlow<HttpAgentRequest> { collection("devConfig").document(agentId) }.collectLatest { req ->
            println("Request: ${req.request.url}")
            runCatching {
                val res = httpClient.request<HttpResponse> {
                    url(req.request.url)
                    method = HttpMethod(req.request.method.methodString)
                    req.request.headers.map { (k, v) ->
                        println("$k:$v")
                        kotlin.runCatching { header(k, v) }.onFailure { it.printStackTrace() }
                    }
                    if (req.request.body != null) {
                        body = req.request.body
                    }
                }
                val resBody = res.readText()
                val r = FsHttpResponse(
                        status = res.status.value,
                        httpVer = res.version.toString(),
                        headers = res.headers.toMap().map { (k, v) -> k to v[0] }.toMap(),
                        body = resBody,
                )
                firestore.collection("devStatus").document(agentId).set(r).get()
            }.onFailure {
                it.printStackTrace()
                firestore.collection("devStatus").document(agentId).set(mapOf("stacktrace" to it.stackTraceToString()))
            }
        }
        delay(5000)
    } while (isActive)
}

