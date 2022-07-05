import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.Text
import kotlin.js.RegExp


@ExperimentalJsExport
@JsExport
fun r() {
    val regexTable = mapOf("([aA])" to "$1")
    replaceNodeText(document.body) { sed(it, regexTable) }
}

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

fun sed(str: String, regexTable: Map<String, String>) = regexTable.entries.fold(str) { s, (regex, repl) ->
    s.replace(Regex(regex), repl)
}