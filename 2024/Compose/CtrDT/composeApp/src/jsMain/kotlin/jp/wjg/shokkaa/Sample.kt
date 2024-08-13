package jp.wjg.shokkaa

import kotlinx.browser.window

@OptIn(ExperimentalJsExport::class)
@JsExport
fun greeting(name:String) {
    window.alert("Hello $name!")
}
