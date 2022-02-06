import kotlinx.browser.window
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch

/**
 * fetch()のサンプル
 * https://blog.tagbangers.co.jp/2020/11/17/spring-react-with-kotlin
 */
@DelicateCoroutinesApi
@ExperimentalJsExport
@JsExport
fun test() {
    GlobalScope.launch {
        val url = "http://35.184.123.90:9022/srdm/issues.json"
        val data = window.fetch(url).await().text().await()
        println(data)
        //TODO
    }
}