import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.content.TextContent
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.ktor.utils.io.*

fun main(args: Array<String>) {
    val port = args.toList().getOrNull(0)?.toInt() ?: 8080
    val server = embeddedServer(CIO, port = port, module = Application::module)
    println("Start proxy server port:$port")
    server.start(wait = true)
}

fun Application.module() {
    val client = HttpClient()

    intercept(ApplicationCallPipeline.Call) {
        val rqUrl = Url(call.request.uri)
        println("rqUrl=$rqUrl")
        println("rqUrl.fragment=${rqUrl.fragment}")
        println("rqUrl.pathSegments=${rqUrl.pathSegments}")
        val tgUrl = rqUrl.parameters["url"] ?: rqUrl.pathSegments.getOrNull(1) ?: "https://google.com"
        val u = URLBuilder(rqUrl).apply { pathSegments.drop(1) }.build()
        println(u)

        println("tgUrl=$tgUrl")

        val response = client.request(tgUrl)
        val proxiedHeaders = response.headers
        val location = proxiedHeaders[HttpHeaders.Location]
        val contentType = proxiedHeaders[HttpHeaders.ContentType]
        val contentLength = proxiedHeaders[HttpHeaders.ContentLength]

//        fun String.stripWikipediaDomain() = this.replace(Regex("(https?:)?//\\w+\\.wikipedia\\.org"), "")
//
//        if (location != null) {
//            call.response.header(HttpHeaders.Location, location.stripWikipediaDomain())
//        }

        when {
            contentType?.startsWith("text/html") == true -> {
                val text = response.bodyAsText()
//                val filteredText = text.stripWikipediaDomain()
                val filteredText = text
                call.respond(
                    TextContent(
                        filteredText,
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
                        appendAll(proxiedHeaders.filter { key, _ ->
                            !key.equals(
                                HttpHeaders.ContentType,
                                ignoreCase = true
                            ) && !key.equals(HttpHeaders.ContentLength, ignoreCase = true)
                        })
                    }
                    override val status: HttpStatusCode = response.status
                    override suspend fun writeTo(channel: ByteWriteChannel) {
                        response.bodyAsChannel().copyAndClose(channel)
                    }
                })
            }
        }
    }
}