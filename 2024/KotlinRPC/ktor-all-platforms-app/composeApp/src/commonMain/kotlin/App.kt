import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.ktor.client.*
import io.ktor.http.*
import jp.wjg.shokkaa.container.*
import kotlinx.datetime.Clock.System.now
import kotlinx.rpc.krpc.ktor.client.installRPC
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.krpc.streamScoped
import kotlinx.rpc.withService

expect val DEV_SERVER_HOST: String

val client by lazy { HttpClient { installRPC() } }

@Composable
fun App() {
    var serviceOrNull: UserService? by remember { mutableStateOf(null) }
    var statusOrNull by remember { mutableStateOf<CtStatus?>(null) }
    var errorMessage by remember { mutableStateOf("Test") }

    LaunchedEffect(Unit) {
        serviceOrNull = client.rpc {
            url {
                host = DEV_SERVER_HOST
                port = 8080
                encodedPath = "/api"
            }
            rpcConfig { serialization { json() } }
        }.withService()
    }

    val service = serviceOrNull ?: return CircularProgressIndicator()
    LaunchedEffect(Unit) {
        streamScoped { service.updateStatus().collect { statusOrNull = it } }
    }

    val status = statusOrNull ?: return CircularProgressIndicator()
//    val status = CtStatus(images = listOf(), containers = listOf(), tasks = listOf())
    var req by remember { mutableStateOf<(suspend () -> Unit)?>(null) }
    LaunchedEffect(req) { req?.invoke();req = null }

    @Composable
    fun imageItem(img: Image) = Card {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = {
                req = { statusOrNull = service.runContainer(img.id, "XXXX-${now().toEpochMilliseconds()}", listOf()) }
            }) { Icon(Icons.Default.PlayArrow, "run") }
            Text(img.id)
            var removing by remember { mutableStateOf(false) }
            IconButton(onClick = {
                removing = true
                req = {
                    statusOrNull = service.removeImage(img.id)
                    removing = false
                }
            }) {
                Icon(Icons.Default.Delete, "delete")
                if (removing) CircularProgressIndicator()
            }
        }
    }

    @Composable
    fun containerItem(ctn: Container) = Card {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            var removing by remember { mutableStateOf(false) }
            IconButton(onClick = {
                req = { statusOrNull = service.execTask(ctn.id) }
            }) { Icon(Icons.Default.PlayArrow, "run") }
            Text(ctn.id)
            Text(ctn.imageId)
            IconButton(onClick = {
                removing = true
                req = {
                    statusOrNull = service.removeContainer(ctn.id)
                    removing = false
                }
            }) {
                Icon(Icons.Default.Delete, "delete")
                if (removing) CircularProgressIndicator()
            }
        }
    }

    @Composable
    fun taskItem(task: Task) = Card {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(task.id)
            Text(task.processId)
            Text(task.status)
            var removing by remember { mutableStateOf(false) }
            IconButton(onClick = {
                removing = true
                req = {
                    runCatching {
                        statusOrNull = service.killTask(task.id)
                    }.onFailure {errorMessage="Kill taks Error" }
                    removing = false
                }
            }) {
                Icon(Icons.Default.Delete, "delete")
                if (removing) CircularProgressIndicator()
            }
        }
    }

    @Composable
    fun AppPanel() = Column(Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        var showDialog by remember { mutableStateOf(false) }
        var running by remember { mutableStateOf(false) }
        if (status.images.isEmpty()) Text("No Images.")
        else status.images.forEach { imageItem(it) }
        IconButton(onClick = { showDialog = true }) {
            Icon(Icons.Default.Add, "Pull Image")
            if (running) CircularProgressIndicator()
        }
        if (errorMessage.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { errorMessage = "" },
                buttons = { Button(onClick = { errorMessage = "" }) { Text("OK") } },
                title = { Text("Error") },
                text = { Text(errorMessage) })
        }
        if (showDialog) pullImageDialog(onClose = { showDialog = false }) {
            running = true
            req = {
                showDialog = false
                statusOrNull = service.pullImage(it)
                running = false
            }
        }

        if (status.containers.isEmpty()) Text("No Container.")
        else status.containers.forEach { containerItem(it) }
        if (status.tasks.isEmpty()) Text("No Task.")
        else status.tasks.forEach { taskItem(it) }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Containerd Console") },
                    actions = {
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuExpanded = !menuExpanded }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(onClick = { }) { Text("Server Information") }
//                            DropdownMenuItem(onClick = { /* Handle action */ }) { Text("Action 2") }
                        }
                    })
            },
        ) {
            AppPanel()
        }
    }
}

@Composable
fun informationDialog(onClose: () -> Unit, cont: @Composable ColumnScope.() -> Unit) =
    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth()) {
            var id by remember { mutableStateOf("") }
            Column(modifier = Modifier.padding(8.dp)) {
                cont()
            }
        }
    }


@Composable
fun pullImageDialog(onClose: () -> Unit, onOk: (String) -> Unit) = Dialog(onDismissRequest = onClose) {
    Card(modifier = Modifier.fillMaxWidth()) {
        var id by remember { mutableStateOf("") }
        Column(modifier = Modifier.padding(8.dp)) {
            TextField(
                value = id,
                label = { Text("Image ID:") },
                onValueChange = { id = it },
                modifier = Modifier.fillMaxWidth().onKeyEvent { e ->
                    if (e.key == Key.Enter) onOk(id)
                    true
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onOk(id) }) { Text("Pull") }
                Button(onClick = onClose) { Text("Cancel") }
            }
        }
    }
}
