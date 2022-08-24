import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.dom.create
import kotlinx.html.js.body
import kotlinx.html.p

suspend fun main() {
    val myDiv = document.create.body {
        p { +"Hello HTML DSL." }
    }
    document.write(myDiv.outerHTML)

    val httpClient = HttpClient()
    val r = httpClient.get<String>("test.ods")

    window.alert(r)

}