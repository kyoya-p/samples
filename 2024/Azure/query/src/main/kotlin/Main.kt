import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.serialization.Serializable
import okio.FileSystem
import okio.Path.Companion.toPath

fun main() = application {
    Window(
        title = "Mongo Query Sample",
        state = rememberWindowState(width = 680.dp, height = 800.dp),
        onCloseRequest = ::exitApplication
    ) { App() }
}


inline fun <reified T : @Serializable Any> store(
    appName: String,
    dataName: String = "app",
): KStore<T> {
    val homeDir = runCatching { System.getenv().let { "${it["HOMEDRIVE"]}${it["HOMEPATH"]}" } }.getOrElse { "." }
    val appPath = homeDir.toPath().resolve(".$appName")
    FileSystem.SYSTEM.createDirectory(appPath)
    return storeOf<T>(appPath.resolve("$dataName.json"))
}

@Composable
inline fun <reified T : @Serializable Any> AppSync(
    appName: String,
    crossinline initData: () -> T,
    dataName: String = "app",
    crossinline op: @Composable (MutableState<T>) -> Unit
) {
    val store = store<T>(appName, dataName)
    val app = remember { mutableStateOf<T?>(null) }
    val f = @Composable {
        LaunchedEffect(Unit) { store.updates.collect { app.value = it ?: initData() } }
        LaunchedEffect(app.value) { if (app.value != null) store.set(app.value) }
        if (app.value != null) @Suppress("UNCHECKED_CAST") op(app as MutableState<T>)
        else CircularProgressIndicator(modifier = Modifier.fillMaxSize())
    }
    f()
}
