import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import io.github.xxfast.kstore.file.storeOf
import kotlinx.serialization.Serializable
import okio.FileSystem
import okio.Path.Companion.toPath

expect val userDir: String
fun appDir() = "$userDir/.snmp-desktop"

inline fun <reified T : @Serializable Any> dataStore(app: String) = storeOf<T>("${appDir()}/$app.json".toPath())

@Serializable
data class AppMain(var page: String = "AGENT")

@Composable
fun App(window: ComposeWindow) = MaterialTheme {
    with(FileSystem.SYSTEM) { createDirectory(appDir().toPath()) }
    var app by remember { mutableStateOf(AppMain()) }
//    val store: KStore<AppMain> = dataStore("main")
    when (app.page) {
        "AGENT" -> capturePage(window)
        "SCANNER" -> scannerPage()
    }
}


internal expect fun openUrl(url: String?)