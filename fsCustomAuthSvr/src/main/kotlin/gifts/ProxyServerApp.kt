import gifts.RspRequest
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.content.TextContent
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import io.ktor.utils.io.*

/*
https://github.com/ktorio/ktor-samples/blob/1.3.0/other/reverse-proxy/src/ReverseProxyApplication.kt
 */
fun main(args: Array<String>) {
    embeddedServer(Netty, port = 8081) {
        install(Locations)
        routing {
            get<RspRequest> { rspReq ->
                println("Req: ${this.context.request.uri}")
                runCatching {
                    //val client = HttpClient()
                    val response = HttpClient().request<HttpResponse>("https://asahi.com")
                    val proxiedHeaders = response.headers
                    val location = proxiedHeaders[HttpHeaders.Location]
                    val contentType = proxiedHeaders[HttpHeaders.ContentType]
                    val contentLength = proxiedHeaders[HttpHeaders.ContentLength]

                    if (location != null) {
                        call.response.header(HttpHeaders.Location, location)
                    }
                    when {
                        contentType?.startsWith("text/html") == true -> {
                            val text = response.readText()
                            call.respond(
                                    TextContent(
                                            text,
                                            ContentType.Text.Html.withCharset(Charsets.UTF_8),
                                            response.status
                                    )
                            )
                        }
                        else -> {
                            call.respond(object : OutgoingContent.WriteChannelContent() {
                                override val contentLength: Long? = contentLength?.toLong()
                                override val contentType: ContentType? = contentType?.let { ContentType.parse(it) }
                                override val headers: Headers = Headers.build {
                                    appendAll(proxiedHeaders.filter { key, _ -> !key.equals(HttpHeaders.ContentType, ignoreCase = true) && !key.equals(HttpHeaders.ContentLength, ignoreCase = true) })
                                }
                                override val status: HttpStatusCode? = response.status
                                override suspend fun writeTo(channel: ByteWriteChannel) {
                                    response.content.copyAndClose(channel)
                                }
                            })
                        }
                    }
                }.onFailure { it.printStackTrace() }
            }
        }
    }.start(wait = true)
}

