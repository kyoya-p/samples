import kotlinx.browser.document
import kotlinx.html.body
import kotlinx.html.dom.create
import kotlinx.html.p

fun main() {
    val myDiv = document.create.body {
        p { +"Hello HTML DSL." }
    }
    document.write(myDiv.outerHTML)
}
