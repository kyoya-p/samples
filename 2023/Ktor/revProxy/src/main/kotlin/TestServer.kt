import io.ktor.http.ContentType.Image.SVG
import io.ktor.http.ContentType.Text.Html
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import okio.FileSystem
import okio.Path.Companion.toPath

fun Application.testTargetModule() = routing {
    val htmlSample1 = """
        |<img src='/m/ok.svg'/>/m/ok.svg</br>
        |<img src='ok.svg'/>ok.svg</br>
        |<img src='./ok.svg'/>./ok.svg<br/>
        |<img src='../ok.svg'/>../ok.svg<br/>
        |<img src='../m/ok.svg'/>../m/ok.svg<br/>""".trimMargin()
    get("/m/sample1") { call.respondText(htmlSample1, Html) }
    get("/m/sample1/") { call.respondText(htmlSample1, Html) }
    get("/m/ok.svg") { call.respondBytes(FileSystem.SYSTEM.read("ok.svg".toPath()) { readByteArray() }, SVG) }
    post { call.respondText { "Your request url is: ${call.request.uri}" } }
}
