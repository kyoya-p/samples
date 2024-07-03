import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
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
import kotlin.streams.asSequence

val appName = "query"

fun main() = application {
//    val logger: Logger = Logger.getLogger("org.mongodb.driver")
//    logger.setLevel(Level.OFF)
//    AppSync(appName, { WinSize(680, 800) }, "winsize") { sizeState ->
//        println(sizeState.value)
//    }
//    ProcessBuilder("cmd.exe", "/c", "cd")
//        .redirectOutput(File("aaa.res"))
////        .inheritIO()
//        .start()
//        .waitFor()

    Window(
        title = "Mongo Query",
//            state = rememberWindowState(width = size.width.dp, height = size.height.dp),
        state = rememberWindowState(width = 680.dp, height = 800.dp),
        onCloseRequest = ::exitApplication
    ) { App() }
}

inline fun <reified T : @Serializable Any> store(
    appName: String,
    dataName: String = "app",
): KStore<T> {
    val homeDir = System.getenv("USERPROFILE") ?: "."
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

@Composable
fun shell() {
    var cmd by remember { mutableStateOf("") }
    var out by remember { mutableStateOf("") }
    fun exec(cmd: String) {
        val p = ProcessBuilder(cmd.split(" "))
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
        p.inputStream.bufferedReader().lines().asSequence().filterNotNull().forEach { out = out + it + "\n" }
    }
    Column {
        Row {
            OutlinedTextField(cmd, onValueChange = { cmd = it })
            Button(onClick = { exec(cmd) }) { Text("Run") }
        }
        OutlinedTextField(out, readOnly = true, onValueChange = {})
    }
}