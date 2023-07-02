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
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

fun main(args: Array<String>): Unit = runBlocking {
    val proxyPort = args.toList().getOrNull(0)?.toInt() ?: 8180
    val testTargetPort = args.toList().getOrNull(1)?.toInt() ?: 8181

    embeddedServer(CIO, port = testTargetPort) { testTargetModule() }.start()
    println("Start sample target server port:$testTargetPort")

    val server = embeddedServer(CIO, port = proxyPort, module = Application::module)
    println("Start proxy server port:$proxyPort")
    server.start(wait = true)
}

fun Application.module() {
    val client = HttpClient(io.ktor.client.engine.cio.CIO) {
        engine {
            https {
                trustManager = object : X509TrustManager {
                    override fun getAcceptedIssuers() = null
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                }
            }
        }
    }
    intercept(ApplicationCallPipeline.Call) {
        suspend fun fallback() {
            println("Fallbacked")
            val fallbackPage = """
                <form><input type="text" name="url"/></form>
            """.trimIndent()
            call.respond(
                TextContent(
                    fallbackPage,
                    ContentType.Text.Html.withCharset(Charsets.UTF_8),
                )
            )
        }

        suspend fun redirectTo(url: Url) {
            println("redirectTo($url)")
            call.response.cookies.append("X-230701-Target-Url", "${url.protocol.name}://${url.hostWithPort}")
            call.respondRedirect(url.fullPath)
        }
        println("q[url]=${call.request.queryParameters["url"]}")
        call.request.queryParameters["url"]?.let { url -> return@intercept redirectTo(Url(url)) }

        println("URL=${call.request.uri}")
        val rqUrl = Url(call.request.uri)
        val tgHostUrl = call.request.cookies["X-230701-Target-Url"] ?: return@intercept fallback()

        println("tgHostUrl = ${tgHostUrl}")

        val tgUrl = URLBuilder(tgHostUrl).apply {
            pathSegments = rqUrl.pathSegments
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
        println(
            "${call.request.httpMethod.value}${rqUrl.pathSegments}/?" +
                    "${call.request.queryParameters["X-230701-Target-Url"]}:" +
                    "${call.request.cookies.rawCookies["X-230701-Target-Url"]}{${recvText.take(20)}} => $tgUrl"
        )

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