import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.res.painterResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Address Book",
        icon = painterResource("icon.png")
    ) {
        Screen()
    }
}