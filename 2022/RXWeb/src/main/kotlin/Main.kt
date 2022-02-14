import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit

//@JsName("CryptoJS")
//external fun CryptoJS()

suspend fun main() {
    load("https://cdnjs.com/libraries/crypto-js")
    val hash = js("CryptoJS.MD5('Message')")
    window.alert(hash)

//    document.onload = {
//        window.alert("onload ${document.body?.outerHTML }")
//    }
//    window.alert(document.body?.outerHTML ?: "nobody")
}

suspend fun load(url: String) = window.fetch(url,
    RequestInit(headers = Headers().apply { set("Access-Control-Allow-Origin", url) })
).await().text().await()

//fun function(jscode: String) = js("window.Function(jscode)")
//fun requireJsModule(module: String) = js("require(module)")

fun loadingLib(path: String) = document.createElement("script").apply {
    setAttribute("src", path)
    setAttribute("type", "javascript")
}
