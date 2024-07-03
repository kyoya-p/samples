import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

@Composable
@Preview
fun App() {
    val winService = KtorDaemon()
    val serviceRunning by remember { mutableStateOf(false) }

    var webApp: ApplicationEngine? by remember { mutableStateOf(null) }
    MaterialTheme {
        Column {
            Button(onClick = { webApp = webApp?.let { it.stop();null } ?: appServer().start() }) {
                Text(if (webApp == null) "Strat Application" else "Stop Application")
            }
            Button(onClick = {}) {
                Text("Register and Start Service")
            }
            Button(onClick = {}) {
                Text("Stop and Unregister Service")
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

fun appServer() = embeddedServer(CIO, port = 8000) {
    routing {
        get("/") {
            call.respondText("Hello, world!")
        }
    }
}
