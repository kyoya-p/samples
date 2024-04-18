import com.microsoft.playwright.*
import com.microsoft.playwright.options.AriaRole
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import kotlinx.serialization.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.Serializable
import kotlin.concurrent.thread


fun main() {
    thread { testServer() }
    Playwright.create().use { playwright ->
        val browser = playwright.chromium().launch(BrowserType.LaunchOptions().apply { headless = false })
        val page = browser.newPage()
        page.navigate("http://localhost:8000/")
        val text = page.getByRole(AriaRole.HEADING).textContent()
        println(text)
        browser.close()
    }
}


val testPage = """
<!DOCTYPE html><html>
<head><title>Hello World</title></head><body>
<h1>Hello World!</h1>
</body></html>
"""

fun testServer() = embeddedServer(CIO, port = 8000) {
    println(System.getProperty("user.dir"))
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    routing {
//        staticFiles("/", File("."))
        get("/") { call.respondText(testPage, ContentType.Text.Html) }
        get("/xxx") { call.respondText(open("https://www.google.co.jp/")) }
        webSocket("/ws") {
            println("opened.")
            val query = receiveDeserialized<LoginReq>()
            println("Query: $query ")
            println("closed.")
        }
    }
}.start(wait = true)

@Serializable
data class LoginReq(val user: String? = null, val password: String? = null)

fun open(url: String) = Playwright.create().use { playwright ->
    val browser = playwright.chromium().launch(BrowserType.LaunchOptions().apply { headless = false })
    val page = browser.newPage()
    page.navigate(url)
    val text = page.title()
    println(text)
    browser.close()
    text
}
