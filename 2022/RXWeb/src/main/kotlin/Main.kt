import kotlinx.browser.document
import kotlinx.browser.window

fun main() {
    //document.write("Hello, world!")
    document.onload={
        window.alert("aaa")
    }

}

//suspend fun load(url: String) = window.fetch(url).await().text().await()
//fun function(jscode: String) = js("window.Function(jscode)")
//fun requireJsModule(module: String) = js("require(module)")

fun loadingLib() = document.createElement("script").apply {
    setAttribute("src", "https://cdnjs.com/libraries/crypto-js")
    setAttribute("type", "javascript")
}
