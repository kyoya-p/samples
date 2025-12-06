import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import jp.wjg.shokkaa.snmp.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "pau",
    ) {
        App()
    }
}