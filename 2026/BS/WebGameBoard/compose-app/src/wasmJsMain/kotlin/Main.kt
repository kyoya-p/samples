import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow

// Global JS bridge for logging
@JsFun("(msg) => window.log(msg)")
external fun jsLog(msg: String)

fun log(msg: String) {
    jsLog(msg)
    println(msg)
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(canvasElementId = "compose-target") {
        App()
    }
}
