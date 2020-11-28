package gifts

import com.google.cloud.firestore.FirestoreOptions
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@KtorExperimentalLocationsAPI
@Location("/rsp/{ch}/{url}")
data class RspRequest(val url: String, val ch: String)

@KtorExperimentalLocationsAPI
suspend fun PipelineContext<Unit, ApplicationCall>.proxy(req: RspRequest, op: suspend (Map<String, Any>) -> Unit) {
    println("Req: ${req.url}")
    val method = when (call.request.httpMethod) {
        HttpMethod.Get -> FsHttpRequest.Method.GET
        HttpMethod.Post -> FsHttpRequest.Method.POST
        else -> FsHttpRequest.Method.GET
    }
    val headers = call.request.headers
    val fsReq = HttpAgentRequest(
            request = FsHttpRequest(
                    url = req.url,
                    method = method,
                    headers = headers.toMap().map { (k, v) -> k to v[0] }.toMap(),
                    body = "", //TODO
            )
    )
    val db = FirestoreOptions.getDefaultInstance().service!!

    db.collection("devConfig").document(req.ch).set(fsReq).get() // TODO: blocking
    db.collection("devStatus").document(req.ch).get().get().data?.let { res ->
        val body = res["body"] as String //TODO
        op(res)
    }
}

/*
http://a.b.c:8080/?a=5&b=4
http%3A%2F%2Fa.b.c%3A8080%2F%3Fa%3D5%26b%3D4
 */

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
        val body: String,
) {
    enum class Method(val methodString: String) {
        GET("GET"),
        POST("POST"),
    }
}

@Serializable
data class Schedule(
        val limit: Int = 1, //　回数は有限に。失敗すると破産するし
        val interval: Long = 0,
)

@Serializable
data class FsHttpResponse(
        val status: Int,
        val httpVer: String,
        val headers: Map<String, String> = mapOf(),
        val body: String,
)


//inline fun <reified T : Any> JsonObject.decode() = Json { ignoreUnknownKeys = true }.decodeFromJsonElement<T>(this.toJsonObject())

fun Map<String, Any>.toJsonObject(): JsonObject = buildJsonObject {
    forEach { (k, v) ->
        @Suppress("UNCHECKED_CAST")
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
        @Suppress("UNCHECKED_CAST")
        when (v) {
            is Number -> add(v)
            is String -> add(v)
            is Boolean -> add(v)
            is Map<*, *> -> add((v as Map<String, Any>).toJsonObject())
            is List<*> -> add((v as List<Any>).toJsonArray())
        }
    }
}


