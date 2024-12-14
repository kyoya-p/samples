import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import jp.wjg.shokkaa.container.ProcessResult
import jp.wjg.shokkaa.container.UserService
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.streamScoped

@Composable
fun App() {
    var serviceOrNull: UserService? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) { serviceOrNull = client.service() }
    val service = serviceOrNull ?: return CircularProgressIndicator(color = Color.Magenta)
    var statusOrNull: CtStatus? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) { streamScoped { statusOrNull = service.getStatus() } }
    val ctrStatus = statusOrNull ?: return CircularProgressIndicator(color = Color.Blue)

    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    @Composable
    fun CtrActionButton(icon: ImageVector, action: suspend () -> ProcessResult) {
        var busy by remember { mutableStateOf(false) }
        IconButton(onClick = {
            busy = true
            scope.launch {
                val r = action()
                statusOrNull = service.getStatus()
                if (r.exitCode != 0) message = """ResultCode: ${r.exitCode}
                    |stdout: ${r.stdout.joinToString("\n")}
                    |stderr: ${r.stderr.joinToString("\n")}
                    |""".trimMargin()
                busy = false
            }
        }) {
            Icon(icon, "")
            if (busy) CircularProgressIndicator()
        }
    }

    @Composable
    fun CtImage.item() = AppRow {
        Icon(Icons.Outlined.Home, "")
        CtrActionButton(Icons.Default.PlayArrow) { runContainer() }
        AppDialogButton(Icons.Default.Settings) { close ->
            var fOpts by remember { mutableStateOf(opts ?: "--detach") }
            var fCid by remember { mutableStateOf(ctrId ?: "") }
            var fArgs by remember { mutableStateOf(args ?: "") }
            Column(Modifier.padding(8.dp).fillMaxWidth()) {
                AppTextField(fOpts, label = { Text("Container Run Options") }, onValueChange = { fOpts = it })
                AppTextField(fCid, label = { Text("Default Container Id") }, onValueChange = { fCid = it })
                AppTextField(fArgs, label = { Text("Args for container shell") }, onValueChange = { fArgs = it })
                AppRow {
                    @Composable
                    fun OptCB(vararg fl: String) = AppCheckbox(fOpts.split1().any { fl.contains(it) }, fl[0]) {
                        fOpts = when (it) {
                            false -> fOpts.split1().filterNot { fl.contains(it) }
                            true -> fOpts.split1() + fl[0]
                        }.joinToString(" ")
                    }
                    OptCB("--detach")
                    OptCB("--rm")
                    Box(Modifier.weight(1f))
                    val save = { opts = fOpts;ctrId = fCid;args = fArgs }
                    CtrActionButton(Icons.Default.PlayArrow) { save();runContainer() }
                    Button(onClick = { save();close() }) { Text("Save") }
                    Button(onClick = { close() }) { Text("Cancel") }
                }
            }
        }
        SelectionContainer(Modifier.weight(1f)) { Text(id) }
        IconButton(onClick = { message = this@item.toJson() }) { Icon(Icons.Default.Info, "") }
        CtrActionButton(Icons.Default.Delete) { remove() }
    }

    @Composable
    fun CtContainer.item() = AppRow {
        Icon(Icons.Default.Star, "")
        Text(id, Modifier.weight(.2f))
        Text(imgId, modifier = Modifier.weight(0.6f))
        Row(modifier = Modifier.weight(0.2f), verticalAlignment = Alignment.CenterVertically) {
            fun CtContainer.status() = ctrStatus.tasks[id]?.status ?: "NO_TSK"
            Text(status())
            CtrActionButton(Icons.Default.PlayArrow) { start() }
            CtrActionButton(Icons.Default.Close) { killTask();removeTask() }
            CtrActionButton(Icons.Default.Delete) { remove() }
        }
    }

    @Composable
    fun AppPanel() = Column(Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        var showDialog by remember { mutableStateOf(false) }
        var running by remember { mutableStateOf(false) }

        // Images
        if (ctrStatus.images.isEmpty()) Text("No Images.")
        else ctrStatus.images.forEach { it.item() }
        IconButton(onClick = { showDialog = true }) {
            Icon(Icons.Default.Add, "Pull Image")
            if (running) CircularProgressIndicator()
            if (showDialog) PullImgDialog(onClose = { showDialog = false }) { id, opts ->
                running = true
                scope.launch {
                    showDialog = false
                    service.ctr("i", "pull", *opts.split2(), id)
                    statusOrNull = service.getStatus()
                    running = false
                }
            }
        }

        // Containers
        if (ctrStatus.containers.isEmpty()) Text("No Container.")
        else ctrStatus.containers.forEach { ctr -> ctr.item() }
        if (message.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { message = "" },
                buttons = { Row(Modifier.padding(8.dp)) { Button(onClick = { message = "" }) { Text("OK") } } },
                title = { Text("Error") },
                text = { Text(message) })
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
                        }
                    })
            },
            floatingActionButton = {
                var show by remember { mutableStateOf(false) }
                if (show) PullImgDialog({ show = false }) { id, opts ->
//                    service.ctr()
                }
                FloatingActionButton(onClick = { show = true }) { Icon(Icons.Default.Add, "add") }
            }
        ) {
            AppPanel()
        }
    }
}

@Composable
fun AppTextField(value: String, label: @Composable() (() -> Unit)? = null, onValueChange: (String) -> Unit) =
    TextField(value = value, label = label, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth())

@Composable
fun AppRow(content: @Composable RowScope.() -> Unit) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxWidth(),
    content = content
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppCheckbox(value: Boolean, label: String, onCheckedChange: (Boolean) -> Unit) = Row(
    verticalAlignment = Alignment.CenterVertically
) {
//    Checkbox(value, onCheckedChange = onCheckedChange)
//    Text(label)
    FilterChip(value, onClick = { onCheckedChange(!value) }) { Text(label) }
}

@Composable
fun <T> AppDialogButton(icon: ImageVector, action: @Composable (close: () -> Unit) -> T) {
    var show by remember { mutableStateOf(false) }
    IconButton(onClick = { show = true }) { Icon(icon, "") }
    if (show) Dialog(onDismissRequest = { show = false }) {
        Card(modifier = Modifier.fillMaxWidth()) { action { show = false } }
    }
}

@Composable
fun PullImgDialog(onClose: () -> Unit, onOk: (id: String, opts: String) -> Unit) = Dialog(onDismissRequest = onClose) {
    Card(modifier = Modifier.fillMaxWidth()) {
        var id by remember { mutableStateOf(pullImageId ?: "") }
        var opts by remember { mutableStateOf(pullImageOpts ?: "") }
        Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
            AppTextField(id, label = { Text("Image ID:") }, onValueChange = { id = it })
            AppTextField(opts, label = { Text("Pull Options:") }, onValueChange = { opts = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { pullImageId = id;pullImageOpts = opts; onOk(id, opts) }) { Text("Pull") }
                Button(onClick = onClose) { Text("Cancel") }
            }
        }
    }
}
