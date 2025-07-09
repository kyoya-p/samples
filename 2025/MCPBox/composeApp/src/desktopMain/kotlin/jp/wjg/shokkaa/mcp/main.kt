package jp.wjg.shokkaa.mcp

import PreferenceDemo
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.singleWindowApplication

//fun main() = application {
//    Window(
//        onCloseRequest = ::exitApplication,
//        title = "MCPBox",
//    ) {
//        App()
//    }
//}

fun main() = singleWindowApplication {
    val scope = rememberCoroutineScope()
    androidx.compose.material.MaterialTheme {
        PreferenceDemo(scope)
    }
}
