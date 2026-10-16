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
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

const val TARGET_COOKIE = "X-230701-Target-Url"

fun main(args: Array<String>): Unit = runBlocking {
    val pathProxyPort = args.toList().getOrNull(0)?.toInt() ?: 8180
    val testTargetPort = args.toList().getOrNull(1)?.toInt() ?: 8181
    val cookieProxyPort = args.toList().getOrNull(2)?.toInt() ?: 8380

    embeddedServer(CIO, host = "127.0.0.1", port = testTargetPort) { testTargetModule() }.start()
    println("Start sample target server port:$testTargetPort")

    embeddedServer(CIO, host = "127.0.0.1", port = cookieProxyPort, module = Application::cookieProxyModule).start()
    println("Start cookie proxy server port:$cookieProxyPort")

    val server = embeddedServer(CIO, host = "127.0.0.1", port = pathProxyPort, module = Application::pathProxyModule)
    println("Start path proxy server port:$pathProxyPort")
    server.start(wait = true)
}

// 注意: サンプル用。証明書検証を無効化しているため、ローカル(127.0.0.1)限定で使うこと
val proxyClient = HttpClient(io.ktor.client.engine.cio.CIO) {
    followRedirects = false // 3xxはそのままブラウザへ返す
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

// パス方式: /url/{URL}/... の{URL}を転送先とする
fun Application.pathProxyModule() {
    intercept(ApplicationCallPipeline.Call) {
        val rqUrl = Url(call.request.uri)
        call.request.queryParameters["url"]?.let { url -> return@intercept call.respondRedirect("/url/${url.encodeURLPathPart()}/") }
        if (!(rqUrl.pathSegments.size >= 3 && rqUrl.pathSegments[1] == "url")) {
            return@intercept call.respondText("<h1>Error!</h1>", ContentType.Text.Html, HttpStatusCode.NotAcceptable)
        }

        val tgUrlHost = rqUrl.pathSegments[2]
        val tgUrl = URLBuilder(tgUrlHost).apply {
            val rqPath = rqUrl.pathSegments
            pathSegments += rqPath.drop(3)
            parameters.appendAll(rqUrl.parameters)
        }.build()

        call.proxyTo(tgUrl)
    }
}

// Cookie方式: Cookie(X-230701-Target-Url)のオリジンを転送先とする
fun Application.cookieProxyModule() {
    intercept(ApplicationCallPipeline.Call) {
        suspend fun redirectTo(url: Url) {
            println("Redirect to: $url")
            call.response.cookies.append(TARGET_COOKIE, "${url.protocol.name}://${url.hostWithPort}")
            call.respondRedirect((if (url.isRelativePath) "/" else "") + url.fullPath)
        }
        call.request.queryParameters["url"]?.let { url -> return@intercept redirectTo(Url(url)) }

        val rqUrl = Url(call.request.uri)
        val tgOriginUrl =
            Url(call.request.cookies[TARGET_COOKIE] ?: return@intercept call.respond(HttpStatusCode.BadRequest))
        val tgUrl = URLBuilder(rqUrl).apply {
            host = tgOriginUrl.host
            port = tgOriginUrl.port
            protocol = tgOriginUrl.protocol
        }.build()

        call.proxyTo(tgUrl)
    }
}

// 共通: tgUrlへリクエストを転送し、応答をそのまま返す
suspend fun ApplicationCall.proxyTo(tgUrl: Url) {
    val skipHeaders = listOf(
        HttpHeaders.ContentType, HttpHeaders.ContentLength, HttpHeaders.TransferEncoding,
        // hop-by-hop
        HttpHeaders.Connection, HttpHeaders.KeepAlive, HttpHeaders.Upgrade, HttpHeaders.TE,
        HttpHeaders.Trailer, HttpHeaders.ProxyAuthenticate, HttpHeaders.ProxyAuthorization,
    )
    fun Headers.myBuilder() = Headers.build {
        appendAll(filter { key, _ -> skipHeaders.none { key.equals(it, true) } })
    }

    val recvText = receiveText()
    val response = proxyClient.request(tgUrl) {
        method = request.httpMethod
        headers {
            appendAll(request.headers.myBuilder())
            remove(HttpHeaders.Host) // 転送先のHostはURLから設定
        }
        setBody(recvText)
    }
    println("${request.httpMethod.value} - ${request.uri} => $tgUrl")
    if (recvText.isNotEmpty()) println("Body: $recvText")

    val proxiedHeaders = response.headers
    val contentType = proxiedHeaders[HttpHeaders.ContentType]
    // HEAD/204/304は本文なし(Content-Lengthを渡すと不整合になる)
    val noBody = request.httpMethod == HttpMethod.Head
            || response.status == HttpStatusCode.NoContent
            || response.status == HttpStatusCode.NotModified
    val contentLength = if (noBody) null else proxiedHeaders[HttpHeaders.ContentLength]

    respond(object : OutgoingContent.WriteChannelContent() {
        override val contentLength: Long? = contentLength?.toLong()
        override val contentType: ContentType? = contentType?.let { ContentType.parse(it) }
        override val headers: Headers = proxiedHeaders.myBuilder()
        override val status: HttpStatusCode = response.status
        override suspend fun writeTo(channel: ByteWriteChannel) {
            if (noBody) channel.close() else response.bodyAsChannel().copyAndClose(channel)
        }
    })
}
