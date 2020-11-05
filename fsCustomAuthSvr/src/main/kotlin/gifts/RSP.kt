package gifts

import com.google.cloud.firestore.Firestore
import db
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable

@KtorExperimentalLocationsAPI
@Location("/rsp/{ch}/{url}")
data class RspRequest(val url: String, val ch: String)

fun RspRequest.rspReq(pipelineContext: PipelineContext<Unit, ApplicationCall>) {
    println(this)

    val method = when (pipelineContext.context.request.httpMethod) {
        HttpMethod.Get -> HttpRequest.Method.GET
        HttpMethod.Post -> HttpRequest.Method.POST
        else -> HttpRequest.Method.GET
    }
    val headers = pipelineContext.context.request.headers
//    val body=pipelineContext.context.request.
    val fsReq = HttpAgentRequest(
            request = HttpRequest(
                    url = url,
                    method = method,
                    headers = headers.toMap().map { (k, v) -> k to v[0] }.toMap(),
                    body = "", //TODO
            )
    )
    println(fsReq)
    db.collection("devConfig").document(ch).set(fsReq).get() // blocking
    val res = db.collection("devStatus").document(ch).get().get() // blocking
    println(res.data)
}

/*
http://a.b.c:8080/?a=5&b=4
http%3A%2F%2Fa.b.c%3A8080%2F%3Fa%3D5%26b%3D4
 */


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
    public enum class Method(val methodString: String) {
        GET("GET"),
        POST("POST"),
    }
}

@Serializable
data class Schedule(
        val limit: Int = 1, //　回数は有限に。失敗すると破産するし
        val interval: Long = 0,
)

