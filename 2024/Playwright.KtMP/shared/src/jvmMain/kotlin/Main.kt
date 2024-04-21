import com.microsoft.playwright.*
import com.microsoft.playwright.options.AriaRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
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
    routing {
        staticFiles("/", File("."))
//        get("/") { call.respondText(testPage, ContentType.Text.Html) }
    }
}.start(wait = false)
