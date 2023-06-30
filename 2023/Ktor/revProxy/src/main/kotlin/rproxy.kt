import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
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
    val testTargetPort = args.toList().getOrNull(1)?.toInt() ?: 8181

    embeddedServer(CIO, port = testTargetPort) { testTargetModule() }.start()
    println("Start sample target server port:$testTargetPort")

    val server = embeddedServer(CIO, port = proxyPort, module = Application::module)
    println("Start proxy server port:$proxyPort")
    server.start(wait = true)
}

fun Application.testTargetModule() = routing {
    val htmlSample1 = """
        |<img src='sun.jpg'/>sun.jpg</br>
        |<img src='./sun.jpg'/>./sun.jpg<br/>
        |<img src='../sun.jpg'/>../sun.jpg<br/>
        |<img src='../m/sun.jpg'/>../m/sun.jpg<br/>
        |<img src='/m/sun.jpg'/>/m/sun.jpg""".trimMargin()
    get("/m/sample1") { call.respondText(htmlSample1, ContentType.Text.Html) }
    get("/m/sample1/") { call.respondText(htmlSample1, ContentType.Text.Html) }
    get("/m/sun.jpg") { call.respondBytes(FileSystem.SYSTEM.read("sun.jpg".toPath()) { this.readByteArray() }) }
    post { call.respondText { "Your request url is: ${call.request.uri}" } }
}


fun Application.module() {
    val client = HttpClient()
    intercept(ApplicationCallPipeline.Call) {
        val rqUrl = Url(call.request.uri)
        val tgUrlHost = rqUrl.pathSegments.getOrNull(1) ?: "http://urlencode.net/"
        val tgUrl = URLBuilder(tgUrlHost).apply {
            if (pathSegments.isEmpty()) pathSegments = rqUrl.pathSegments.take(1) + rqUrl.pathSegments.drop(2)
            else pathSegments += rqUrl.pathSegments.drop(2)
            parameters { rqUrl.parameters }
        }.build()

        fun Headers.myBuilder() = Headers.build {
            appendAll(filter { key, _ ->
                !key.equals(HttpHeaders.ContentType, true)
                        && !key.equals(HttpHeaders.ContentLength, true)
                        && !key.equals(HttpHeaders.TransferEncoding, true)
            })
        }

        val recvText = call.receiveText()
        val response = client.request(tgUrl) {
            method = call.request.httpMethod
            headers { call.request.headers.myBuilder() }
            setBody(recvText)
        }
        println("${call.request.httpMethod}${rqUrl.pathSegments}{${recvText.take(20)}} => $tgUrl")

        val proxiedHeaders = response.headers
//        val location = proxiedHeaders[HttpHeaders.Location]
        val contentType = proxiedHeaders[HttpHeaders.ContentType]
        val contentLength = proxiedHeaders[HttpHeaders.ContentLength]

//        fun String.stripWikipediaDomain() = this.replace(Regex("(https?:)?//\\w+\\.wikipedia\\.org"), "")
//
//        if (location != null) {
//            call.response.header(HttpHeaders.Location, location.stripWikipediaDomain())
//        }

//        when {
//            contentType?.startsWith("text/html") == true -> {
//                val text = response.bodyAsText()
////                val filteredText = text.stripWikipediaDomain()
//                val filteredText = text
//                call.respond(
//                    TextContent(
//                        filteredText,
//                        ContentType.Text.Html.withCharset(Charsets.UTF_8),
//                        response.status
//                    )
//                )
//            }
//
//            else -> {


        call.respond(object : OutgoingContent.WriteChannelContent() {
            override val contentLength: Long? = contentLength?.toLong()
            override val contentType: ContentType? = contentType?.let { ContentType.parse(it) }
            override val headers: Headers = proxiedHeaders.myBuilder()

            //            override val headers = response.headers
            override val status: HttpStatusCode = response.status
            override suspend fun writeTo(channel: ByteWriteChannel) {
                response.bodyAsChannel().copyAndClose(channel)
            }
        })
//            }
//        }
    }
}