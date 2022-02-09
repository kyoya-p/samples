import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.html.dom.create
import kotlinx.html.js.script

/**
 * 暗号化ライブラリ(CDN): https://cdnjs.com/libraries/crypto-js
 */
@ExperimentalJsExport
fun main() {
//    document.onload = {
//        document.body?.append(document.create.script(type = "javascript",
//            src = "https://cdnjs.com/libraries/crypto-js") {}
//        )
//    }
    //val cryptoJS = requireJsModule("crypto-js");
    function("window.alert('aaaa')")()
}


/**
 * fetch()のサンプル
 * https://blog.tagbangers.co.jp/2020/11/17/spring-react-with-kotlin
 */
suspend fun load(url: String) = window.fetch(url).await().text().await()
fun function(jscode: String) = js("window.Function(jscode)")
fun requireJsModule(module: String) = js("require(module)")