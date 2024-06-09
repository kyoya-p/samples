import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.xxfast.kstore.file.storeOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okio.FileSystem
import okio.Path.Companion.toPath

fun main() = application {
    Window(
        title = "AzQuery",
        state = rememberWindowState(width = 600.dp, height =800.dp),
        onCloseRequest = ::exitApplication
    ) { App() }
}

@Composable
inline fun <reified T : @Serializable Any> AppSync(
    appName: String,
    crossinline initData: () -> T,
    dataName: String = "app",
    crossinline op: @Composable (MutableState<T>) -> Unit
) {
    val homeDir = runCatching { System.getenv().let { "${it["HOMEDRIVE"]}${it["HOMEPATH"]}" } }.getOrElse { "." }
    val appPath = homeDir.toPath().resolve(".$appName")
    FileSystem.SYSTEM.createDirectory(appPath)
    val store = storeOf<T>(appPath.resolve("$dataName.json"))
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
@Preview
fun App() = AppSync<AppData>(appName = "query", initData = { AppData() }) { mutableApp ->
    var app by mutableApp

    @Composable
    fun AppData.field(label: String, get: AppData.() -> String, set: AppData.(n: String) -> AppData) =
        OutlinedTextField(get(), label = { Text(label) }, onValueChange = { app = set(it) })
    LaunchedEffect(app.running) {
        if (app.running) {
            launch(Dispatchers.Default) {
                kotlin.runCatching {
                    query(app).collect { doc ->
//                        println(doc.toJson())
                        app = app.copy(result = app.result.apply { add(doc.toJson()) })
                    }
                }.onFailure { app = app.copy(result = app.result.apply { add(it.stackTraceToString()) }) }
                app = app.copy(running = false)
            }
        }
    }
    MaterialTheme {
        Scaffold(floatingActionButton = {
            FloatingActionButton(onClick = { app = app.copy(running = true, result = mutableListOf()) }) {
                when (app.running) {
                    true -> CircularProgressIndicator()
                    else -> Icon(Icons.AutoMirrored.Filled.Send, "send")
                }
            }
        }) {
            Column(modifier = Modifier.padding(8.dp)) {
                app.field("Connection String", { connStr }, { copy(connStr = it) })
                app.field("Database Name", { dbName }, { copy(dbName = it) })
                app.field("Collection Name", { collName }, { copy(collName = it) })
                FilterSettings()
                Text("Results: ${app.result.count()} docs")
                app.result.forEach {
                    Card(modifier = Modifier.fillMaxWidth().padding(1.dp)) { Text(it, maxLines = 1) }
                }
            }
        }
    }
}

@Composable
fun FilterSettings() = AppSync<FilterList>("query", { listOf() }, "filter") { filters0 ->
    var filters by filters0

    @Composable
    fun Filter.field(ix: Int, label: String, get: Filter.() -> String, set: Filter.(n: String) -> Filter) =
        OutlinedTextField(get(), label = { Text(label) },
            onValueChange = { v -> filters = filters.toMutableList().also { it[ix] = set(v) } })

    Column {
        Text("Filter:")
        filters.forEachIndexed { i, it ->
            Row {
                it.field(i, "field", { field }, { copy(field = it) })
                it.field(i, "value", { value }, { copy(value = it) })
                IconButton(onClick = { filters = filters.toMutableList().also { it.removeAt(i) } }) {
                    Icon(Icons.Default.Delete, "del")
                }
            }
        }
        IconButton(onClick = { filters = filters + Filter("", "", "==") }) { Icon(Icons.Default.Add, "add") }
    }
}



