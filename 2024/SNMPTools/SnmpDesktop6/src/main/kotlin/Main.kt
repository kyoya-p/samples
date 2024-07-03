import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import jp.wjg.shokkaa.snmp4jutils.async.createDefaultSenderSnmpAsync
import jp.wjg.shokkaa.winApp2


fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        with(createDefaultSenderSnmpAsync()) {
            winApp2(window)
        }
    }
}
