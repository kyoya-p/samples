import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.coroutines.flow.Flow

//external fun authStateChanged(): Flow<FirebaseUser?>

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
//    authStateChanged().collect{}
    CanvasBasedWindow(canvasElementId = "ComposeTarget") { App() }
}
