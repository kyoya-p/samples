import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow("BS Web Game Board") {
        App()
    }
}

@Composable
fun App() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Battle Spirits Web Game Board", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Compose Multiplatform (Wasm) is working!")
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { /* Nothing */ }) {
                    Text("Ready")
                }
            }
        }
    }
}
