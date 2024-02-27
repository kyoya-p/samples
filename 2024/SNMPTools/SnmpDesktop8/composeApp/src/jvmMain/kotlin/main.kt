import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import org.company.app.App

fun main() = application {
    Window(
        title = "Multiplatform App",
        state = rememberWindowState(width = 360.dp, height = 400.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(360, 600)
        ScanRange()
    }
}