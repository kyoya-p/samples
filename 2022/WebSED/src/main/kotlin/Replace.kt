import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.Text


@ExperimentalJsExport
@JsExport
fun r() {
    replaceNodeText(document.body) { it }
}

@ExperimentalJsExport
@JsExport
fun replaceNodeText(rootElement: Element?, repOp: (String) -> String = { "[$it]" }) {
    var n = rootElement?.firstChild
    while (n != null) {
        if (n is Text) {
            println("[${n.textContent}]")
            n.textContent = n.textContent?.let { repOp(it) }
        } else if (n is Element) {
            replaceNodeText(n)
        }
        n = n.nextSibling
    }
}
