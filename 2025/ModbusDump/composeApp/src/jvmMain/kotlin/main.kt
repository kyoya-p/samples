import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main(args: Array<String>) = if (args.isNotEmpty()) modbusMain (args)
else application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ModbusDump",
    ) { UI() }
}
