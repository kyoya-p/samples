import kotlinx.browser.document
import kotlinx.browser.window

@JsName("CryptoJS")
external fun CryptoJS()

fun main() {
    loadingLib("https://cdnjs.com/libraries/crypto-js")
    val hash = js("CryptoJS.MD5('Message')")
    window.alert(hash)

//    document.onload = {
//        window.alert("onload ${document.body?.outerHTML }")
//    }
//    window.alert(document.body?.outerHTML ?: "nobody")
}

suspend fun load(url: String) = window.fetch(url).await().text().await()
//fun function(jscode: String) = js("window.Function(jscode)")
//fun requireJsModule(module: String) = js("require(module)")

fun loadingLib(path: String) = document.createElement("script").apply {
    setAttribute("src", path)
    setAttribute("type", "javascript")
}
