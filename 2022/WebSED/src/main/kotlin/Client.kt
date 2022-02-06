import kotlinx.html.div
import kotlinx.html.dom.append
import org.w3c.dom.Node
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.Text

fun main() {
    //window.onload = { document.body?.sayHello() }
    r()
}

fun Node.sayHello() {
    append {
        div {
            +"Hello from JS!"
        }
    }
}

@ExperimentalJsExport
@JsExport
fun hello() {
    document.body?.sayHello()
}

