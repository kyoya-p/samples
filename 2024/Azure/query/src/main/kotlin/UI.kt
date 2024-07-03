import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() = AppSync<AppData>(appName = appName, initData = { AppData() }) { mutableApp ->
    var app by mutableApp

    @Composable
    fun AppData.field(label: String, get: AppData.() -> String, set: AppData.(n: String) -> AppData) =
        OutlinedTextField(get(), label = { Text(label) }, onValueChange = { app = set(it) })
    LaunchedEffect(app.running) {
        println("***************************************************************************")
        if (app.running) {
            println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX")
            launch(Dispatchers.Default) {
//                runCatching {
                val filters = store<FilterList>("query", "filter").get() ?: listOf()
                query(app, filters).collect { doc ->
//                    println(doc.toJson())
                    app = app.copy(result = app.result.toMutableList().apply { add(doc.toJson()) })
                }
//                }.onFailure { app = app.copy(result = app.result.apply { add(it.stackTraceToString()) }) }
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
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    app.result.forEach {
                        Card(modifier = Modifier.fillMaxWidth().padding(1.dp)) { Text(it, maxLines = 1) }
                    }
                }
                shell()
            }
        }
    }
}

@Composable
fun App2() {
    val app = MutableStateFlow(AppData())

}

@Composable
fun FilterSettings() = AppSync<FilterList>(appName, { listOf() }, "filter") { filters0 ->
    var filters by filters0

    @Composable
    fun Filter.field(ix: Int, label: String, get: Filter.() -> String, set: Filter.(n: String) -> Filter) =
        OutlinedTextField(value = get(), label = { Text(label) },
            onValueChange = { v -> filters = filters.toMutableList().also { it[ix] = set(v) } })

    Column {
        Text("Filter:")
        filters.forEachIndexed { i, it ->
            Row() {
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



