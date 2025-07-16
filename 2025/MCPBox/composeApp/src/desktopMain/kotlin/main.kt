package jp.wjg.shokkaa.mcp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MCPBox",
//        size = DpSize(width = 800.dp, height = 600.dp)
    ) {
        App()
    }
}