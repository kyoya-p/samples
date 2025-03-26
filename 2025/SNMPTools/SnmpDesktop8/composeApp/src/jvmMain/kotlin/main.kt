import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import okio.Path.Companion.toPath
import java.awt.Dimension

fun main() = application {
    Window(
        title = "SNMP Desktop",
        state = rememberWindowState(width =600.dp, height = 200.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(400, 150)
//        AppNavigator()
        App(window)
    }
}

actual val userDir = System.getenv().let {
    "${it["HOMEDRIVE"]}${it["HOMEPATH"]}".takeIf { it.toPath().toFile().isDirectory } ?: it["HOME"] ?: "."
}
