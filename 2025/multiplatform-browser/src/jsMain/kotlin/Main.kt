import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement

fun main() {
    val root = document.createElement("div") as HTMLElement
    root.textContent = "Hello, Kotlin/JS Browser App!"
    document.body?.appendChild(root)

    val button = document.createElement("button") as HTMLButtonElement
    button.textContent = "Click Me!"
    var i = 0
    button.onclick = { root.textContent = "Button Clicked! ${++i} times." }
    document.body?.appendChild(button)
}