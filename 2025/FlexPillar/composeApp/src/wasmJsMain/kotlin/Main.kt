import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import v2.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() = ComposeViewport(document.body!!) { App() }

