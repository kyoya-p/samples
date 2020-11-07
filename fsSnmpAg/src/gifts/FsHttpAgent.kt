package gifts

import firestoreInterOp.firestoreDocumentFlow
import gdvm.agent.mib.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

@Serializable
data class HttpAgentRequest(
        val schedule: Schedule = Schedule(1),
        val request: HttpRequest,
)

@Serializable
data class HttpRequest(
        val url: String,
        val method: Method = Method.GET,
        val headers: Map<String, String> = mapOf(),
        val body: String,
) {
    enum class Method(val methodString: String) {
        GET("GET"),
        POST("POST"),
    }
}


@InternalCoroutinesApi
fun main(args: Array<String>): Unit = runBlocking {
    val agentDeviceIds = if (args.isEmpty()) listOf("httpAgent1") else args.map { it }
    agentDeviceIds.forEach {
        println("Start Agent ${agentDeviceIds}")
        launch { runHttpAgent(it) }
        println("Terminated Agent ${agentDeviceIds}")
    }
}

val httpClient = HttpClient()

@InternalCoroutinesApi
suspend fun runHttpAgent(agentId: String) {
    do {
        firestore.firestoreDocumentFlow<HttpAgentRequest> { collection("devConfig").document(agentId) }.collectLatest { req ->
            println(req)
            runCatching {
                val res = httpClient.request<String> {
                    url(req.request.url)
                    method = HttpMethod(req.request.method.methodString)
                    req.request.headers.map { (k, v) -> header(k, v) }
                    body = req.request.body
                }
                firestore.collection("devStatus").document(agentId).set(mapOf("response" to res))
            }.onFailure {
                it.printStackTrace()
                firestore.collection("devStatus").document(agentId).set(mapOf("stacktrace" to it.stackTraceToString()))
            }
        }
        delay(5000)
    } while (isActive)
}

