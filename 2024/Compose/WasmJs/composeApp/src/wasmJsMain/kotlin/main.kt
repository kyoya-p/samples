import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

external fun dialog()

@OptIn(ExperimentalComposeUiApi::class)
fun main() = ComposeViewport(document.body!!) {
    Button(onClick = { dialog() }) { Text("alert!") }
}
