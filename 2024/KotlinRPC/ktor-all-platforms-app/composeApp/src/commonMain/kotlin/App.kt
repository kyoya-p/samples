import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.ktor.client.*
import io.ktor.http.*
import jp.wjg.shokkaa.container.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
    class CtImage(val id: String)
    class CtContainer(val id: String, val imgId: String)
    class CtTask(val pId: String, val execId: String, val status: String)
    class CtStatus(val images: List<CtImage>, val containers: List<CtContainer>, val tasks: List<CtTask>)

    var serviceOrNull: UserService? by remember { mutableStateOf(null) }
    var statusOrNull by remember { mutableStateOf<CtStatus?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

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

    val service = serviceOrNull ?: return CircularProgressIndicator(color = Color.Magenta)

    //    LaunchedEffect(Unit) {
//        streamScoped {
//            service.updateStatus().collectLatest {
//                statusOrNull = it
//            }
//        }
//    }
    suspend fun CtImage.runContainer(ctrId: String, vararg args: String) = service.ctr("run", "-d", id, ctrId, *args)
    suspend fun CtImage.remove() = service.ctr("i", "rm", id)
    suspend fun CtContainer.start(ctrId: String) = service.ctr("run", id, ctrId)
    suspend fun CtContainer.remove() = service.ctr("c", "rm", id)

    suspend fun getStatus() = CtStatus(
        images = service.ctr("i", "ls", "-q").stdout.map { CtImage(it.trim()) },
        containers = service.ctr("c", "ls").stdout.drop(1).map { it.split(Regex("\\s+")) }
            .map { CtContainer(it[0], it[1]) },
        tasks = service.ctr("t", "ls").stdout.drop(1).map { it.split(Regex("\\s+")) }
            .map { CtTask(it[0], it[1], it[2]) }
    )

    LaunchedEffect(Unit) {
        streamScoped { statusOrNull = getStatus() }
    }

    val status = statusOrNull ?: return CircularProgressIndicator(color = Color.Blue)

    //    var req by remember { mutableStateOf<(suspend () -> Unit)?>(null) }
//    LaunchedEffect(req) { req?.invoke();req = null }

    @Composable
    fun <T> T.ActionButton(icon: ImageVector, action: suspend T.() -> Unit) {
        var busy by remember { mutableStateOf(false) }
        IconButton(onClick = {
            busy = true
            scope.launch {
                action()
                statusOrNull = getStatus()
                busy = false
            }
        }) {
            Icon(icon, "")
            if (busy) CircularProgressIndicator()
        }
    }


    @Composable
    fun CtImage.item() = Card {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.Home, "")
            ActionButton(Icons.Default.PlayArrow) {
                val r = runContainer("C-${now().toEpochMilliseconds() % 1000}")
                if (r.exitCode != 0) errorMessage = "[Ex: ${r.exitCode} ${r.stdout.joinToString("\n")}]"
            }
            Text(id)
            ActionButton(Icons.Default.Delete) { remove() }
//            var removing by remember { mutableStateOf(false) }
//            IconButton(onClick = {
//                removing = true
//                scope.launch {
////                    statusOrNull = service.removeImage(id)
//                    removing = false
//                }
//            }) {
//                Icon(Icons.Default.Delete, "delete")
//                if (removing) CircularProgressIndicator()
//            }
        }
    }

    @Composable
    fun CtContainer.item() = Card {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
//            IconButton(onClick = { req = { statusOrNull = service.execTask(ctn.id) } }) {
//                Icon(Icons.Default.PlayArrow, "run")
//            }
            Text(id, Modifier.weight(1f))
            Text(imgId, Modifier.weight(1f))
//            IconButton(onClick = {
//                removing = true
//                req = {
//                    statusOrNull = service.removeContainer(ctn.id)
//                    removing = false
//                }
//            }) {
//                Icon(Icons.Default.Delete, "delete")
//                if (removing) CircularProgressIndicator()
//            }
            ActionButton(Icons.Default.Delete) {
                val r = remove()
                if (r.exitCode != 0) errorMessage = "[Ex: ${r.exitCode} ${r.stdout.joinToString("\n")}]"
            }
        }
    }

//    @Composable
//    fun taskItem(task: Task) = Card {
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text(task.ctrId)
//            Text(task.pId)
//            Text(task.status)
//            var removing by remember { mutableStateOf(false) }
//            IconButton(onClick = {
//                removing = true
//                req = {
//                    runCatching {
//                        statusOrNull = service.killTask(task.ctrId)
//                    }.onFailure { errorMessage = "Error: killTask()" }
//                    removing = false
//                }
//            }) {
//                Icon(Icons.Default.Delete, "delete")
//                if (removing) CircularProgressIndicator()
//            }
//        }
//    }

    @Composable
    fun AppPanel() = Column(Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        var showDialog by remember { mutableStateOf(false) }
        var running by remember { mutableStateOf(false) }
//        LaunchedEffect(Unit) {
//            streamScoped {
//                service.updateStatus().collect { images = it.images }
//            }
//        }
        if (status.images.isEmpty()) Text("No Images.")
        else status.images.forEach { it.item() }
        IconButton(onClick = { showDialog = true }) {
            Icon(Icons.Default.Add, "Pull Image")
            if (running) CircularProgressIndicator()
        }
        if (status.containers.isEmpty()) Text("No Container.")
        else status.containers.forEach { it.item() }
//        if (status.tasks.isEmpty()) Text("No Task.")
//        else status.tasks.forEach { taskItem(it) }

        if (errorMessage.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { errorMessage = "" },
                buttons = { Button(onClick = { errorMessage = "" }) { Text("OK") } },
                title = { Text("Error") },
                text = { Text(errorMessage) })
        }
        if (showDialog) pullImageDialog(onClose = { showDialog = false }) {
            running = true
            scope.launch {
                showDialog = false
                service.ctr("i", "pull", it)
                statusOrNull = getStatus()
                running = false
            }
        }
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

