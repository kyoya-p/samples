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
import io.ktor.server.routing.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath

fun main(args: Array<String>): Unit = runBlocking {
    val proxyPort = args.toList().getOrNull(0)?.toInt() ?: 8180
    val SampleTargetPort = args.toList().getOrNull(1)?.toInt() ?: 8181


    embeddedServer(CIO, port = SampleTargetPort) { testTargetModule() }.start()
    println("Start sample target server port:$SampleTargetPort")

    val server = embeddedServer(CIO, port = proxyPort, module = Application::module)
    println("Start proxy server port:$proxyPort")
    server.start(wait = true)
}

fun Application.testTargetModule() {
    val htmlSample = """<img src='./sun.jpg'/> <img src='sun.jpg'/> <img src='/m/sun.jpg'/>"""
    routing {
        get("/m/index.html") { call.respondText(htmlSample, ContentType.Text.Html) }
        get("/m/sun.jpg") { call.respondBytes(FileSystem.SYSTEM.read("sun.jpg".toPath()) { this.readByteArray() }) }
    }
}

fun Application.module() {
    val client = HttpClient()
    intercept(ApplicationCallPipeline.Call) {
        println("req={${call.request.uri},${call.request.httpMethod},${call.request.headers.toMap()}}")
        val rqUrl = Url(call.request.uri)
        val tgUrlHost = rqUrl.pathSegments.getOrNull(1) ?: "http://urlencode.net/"
        val tgUrl = URLBuilder(tgUrlHost).apply {
            pathSegments = pathSegments + rqUrl.pathSegments.take(1) + rqUrl.pathSegments.drop(2)
            parameters { rqUrl.parameters }
        }.build()

        println("$rqUrl => $tgUrl")

        val response = client. request(tgUrl) {
            method = call.request.httpMethod
            headers { call.request.headers }
        }
        val proxiedHeaders = response.headers
//        val location = proxiedHeaders[HttpHeaders.Location]
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