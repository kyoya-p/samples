import kotlinx.html.div
import kotlinx.html.dom.append
import org.w3c.dom.Node
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.NodeFilter
import org.w3c.dom.Text
import org.w3c.dom.Window

fun main() {
    window.onload = { document.body?.sayHello() }
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
    window.alert("Yah")
}
