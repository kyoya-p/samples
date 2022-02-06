import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.Text


@ExperimentalJsExport
@JsExport
fun r(repOp: (String) -> String = { it.reversed() }) {
    replaceNodeText(document.body, repOp)
}

@ExperimentalJsExport
@JsExport
fun replaceNodeText(rootElement: Element?, repOp: (String) -> String) {
    var n = rootElement?.firstChild
    while (n != null) {
        when (n) {
            is Text -> n.textContent = n.textContent?.let { repOp(it) }
            is Element -> replaceNodeText(n, repOp)
        }
        n = n.nextSibling
    }
}
