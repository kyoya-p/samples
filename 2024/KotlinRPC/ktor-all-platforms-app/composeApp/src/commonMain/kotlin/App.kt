import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.ktor.client.*
import io.ktor.http.*
import jp.wjg.shokkaa.container.*
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
    class CtTask(val execId: String, val pId: String, val status: String)
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

    suspend fun CtImage.runContainer(ctrId: String, vararg args: String) = service.ctr("run", "-d", id, ctrId, *args)
    suspend fun CtImage.remove() = service.ctr("i", "rm", id)
    suspend fun CtContainer.start(ctrId: String) = service.ctr("run", id, ctrId)
    suspend fun CtContainer.remove() = service.ctr("c", "rm", id)
    suspend fun CtTask.kill(signal: Int = 9) = service.ctr("t", "kill", "-s", "$signal", execId)

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

    val ctrStatus = statusOrNull ?: return CircularProgressIndicator(color = Color.Blue)

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
            SelectionContainer {
                Text(id)
            }
            ActionButton(Icons.Default.Delete) { remove() }
        }
    }

    @Composable
    fun CtContainer.item() = Card {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Star, "")
            Text(id, Modifier.weight(1f))
            Text(imgId, Modifier.weight(1f))
            ActionButton(Icons.Default.Delete) {
                val r = remove()
                if (r.exitCode != 0) errorMessage = "[Ex: ${r.exitCode} ${r.stdout.joinToString("\n")}]"
            }
        }
    }

    @Composable
    fun CtTask.item() = Card {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(execId)
            Text(pId)
            Text(status)
            ActionButton(Icons.Default.Delete) {
                val r = kill()
                if (r.exitCode != 0) errorMessage = "[Ex: ${r.exitCode} ${r.stdout.joinToString("\n")}]"
            }
        }
    }

    @Composable
    fun AppPanel() = Column(Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        var showDialog by remember { mutableStateOf(false) }
        var running by remember { mutableStateOf(false) }
//        LaunchedEffect(Unit) {
//            streamScoped {
//                service.updateStatus().collect { images = it.images }
//            }
//        }
        if (ctrStatus.images.isEmpty()) Text("No Images.")
        else ctrStatus.images.forEach { it.item() }
        IconButton(onClick = { showDialog = true }) {
            Icon(Icons.Default.Add, "Pull Image")
            if (running) CircularProgressIndicator()
        }

        if (ctrStatus.containers.isEmpty()) Text("No Container.")
        else ctrStatus.containers.forEach { it.item() }

        if (ctrStatus.tasks.isEmpty()) Text("No Task.")
        else ctrStatus.tasks.forEach { it.item() }

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
fun <T> T.showIf(f: Boolean, op: @Composable T.() -> Unit): (Boolean) -> Unit {
    var show by remember { mutableStateOf(false) }
    return { s: Boolean -> show = s }
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

