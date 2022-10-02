import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import java.net.ServerSocket

@Composable
@Preview
fun App() = MaterialTheme {
    var svr by remember { mutableStateOf<ApplicationEngine?>(null) }
    var port by remember { mutableStateOf(0) }

    Scaffold(topBar = { TopAppBar(title = { Text("TCP Port Opener") }) }) {
        Column {
            TextField("$port", onValueChange = { runCatching { port = it.toInt() } })
            Button(onClick = {
                if (svr == null) {
                    svr = server(port)
                    svr?.start()
                } else {
                    svr?.stop()
                    svr = null
                }
            }) {
                if (svr == null) {
                    Text("Start listening")
                } else {
                    Text("Stop listening")
                }
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

fun server(port: Int) = embeddedServer(Netty, port = port) {
    routing {
    }
}
