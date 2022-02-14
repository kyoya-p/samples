import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.CORS
import org.w3c.fetch.RequestInit
import org.w3c.fetch.RequestMode

//@JsName("CryptoJS")
//external fun CryptoJS()

suspend fun main() {
    window.alert("start 1")
    load("https://cdnjs.com/libraries/crypto-js")
    val hash = js("CryptoJS.MD5('Message')")
    window.alert(hash)

//    document.onload = {
//        window.alert("onload ${document.body?.outerHTML }")
//    }
//    window.alert(document.body?.outerHTML ?: "nobody")
}

suspend fun load(url: String) = window.fetch(
    url,
    RequestInit(
        //headers = Headers().apply { set("Access-Control-Allow-Origin", url) },
        mode = RequestMode.CORS,
    ),
).await().text().await()

//fun function(jscode: String) = js("window.Function(jscode)")
//fun requireJsModule(module: String) = js("require(module)")

fun loadingLib(path: String) = document.createElement("script").apply {
    setAttribute("src", path)
    setAttribute("type", "javascript")
}
