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
import io.ktor.client.*
import io.ktor.http.*
import jp.wjg.shokkaa.container.ProcessResult
import jp.wjg.shokkaa.container.UserService
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock.System.now
import kotlinx.rpc.krpc.ktor.client.installRPC
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.krpc.streamScoped
import kotlinx.rpc.withService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.KProperty

expect val DEV_SERVER_HOST: String
expect val DEV_SERVER_PORT: String

val client by lazy { HttpClient { installRPC() } }

@Composable
fun App() {

    class CtImage(val id: String) {
        var runOpts by localStorageList<String>("containerOp.$id")
        var ctrId by localStorageString("containerOp.$id")
    }

    class CtContainer(val id: String, val imgId: String)
    class CtTask(val execId: String, val pId: String, val status: String)
    class CtStatus(val images: List<CtImage>, val containers: List<CtContainer>, val tasks: Map<String, CtTask>)

    var serviceOrNull: UserService? by remember { mutableStateOf(null) }
    var statusOrNull: CtStatus? by remember { mutableStateOf(null) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        serviceOrNull = client.rpc {
            url {
                host = DEV_SERVER_HOST
                port = DEV_SERVER_PORT.toInt()
                encodedPath = "/api"
            }
            rpcConfig { serialization { json() } }
        }.withService()
    }

    val service = serviceOrNull ?: return CircularProgressIndicator(color = Color.Magenta)

    fun <T> List<String>.mkItems(op: (List<String>) -> T) = drop(1).map { it.appSplit() }.map(op)
    suspend fun getStatus() = CtStatus(
        images = service.ctr("i", "ls", "-q").stdout.map { CtImage(it.trim()) },
        containers = service.ctr("c", "ls").stdout.mkItems { CtContainer(it[0], it[1]) },
        tasks = service.ctr("t", "ls").stdout.mkItems { CtTask(it[0], it[1], it[2]) }.associate { it.execId to it }
    )

    LaunchedEffect(Unit) { streamScoped { statusOrNull = getStatus() } }

    val ctrStatus = statusOrNull ?: return CircularProgressIndicator(color = Color.Blue)
    suspend fun CtImage.runContainer(ctrId: String, vararg args: String) = service.ctr("run", "-d", *args, id, ctrId)
    suspend fun CtImage.remove() = service.ctr("i", "rm", id)
    suspend fun CtContainer.remove() = service.ctr("c", "rm", id)
    suspend fun CtContainer.start() = service.ctr("t", "start", "-d", id)
    suspend fun CtContainer.killTask(signal: Int = 9) = service.ctr("t", "kill", "-s", "$signal", id)
    suspend fun CtContainer.removeTask() = service.ctr("t", "rm", id)


    @Composable
    fun AppRow(content: @Composable RowScope.() -> Unit) = Card(Modifier.padding(1.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }

    @Composable
    fun <T> T.CtrActionButton(icon: ImageVector, action: suspend T.() -> ProcessResult) {
        var busy by remember { mutableStateOf(false) }
        IconButton(onClick = {
            busy = true
            scope.launch {
                val r = action()
                statusOrNull = getStatus()
                if (r.exitCode != 0) errorMessage = """ResultCode: ${r.exitCode}
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
    fun <T> DialogButton(icon: ImageVector, action: @Composable (close: T.() -> Unit) -> Unit) {
        var show by remember { mutableStateOf(false) }
        IconButton(onClick = { show = true }) { Icon(icon, "") }
        if (show) Dialog(onDismissRequest = { show = false }) {
            Card(modifier = Modifier.fillMaxWidth()) { action { show = false } }
        }
    }

    @Composable
    fun CtImage.item() = AppRow {
        Icon(Icons.Outlined.Home, "")
        CtrActionButton(Icons.Default.PlayArrow) {
            runContainer(
                ctrId ?: "C${now().toEpochMilliseconds() % 10000}",
                *((runOpts ?: listOf()).toTypedArray())
            )
        }
        DialogButton(Icons.Default.Settings) { close ->
            var opts by remember { mutableStateOf(runOpts?.joinToString(" ") ?: "") }
            var cid by remember { mutableStateOf(ctrId ?: "") }
            Column(Modifier.padding(8.dp).fillMaxWidth()) {
                AppTextField(cid, label = { Text("Default Container Id") }, onValueChange = { cid = it })
                AppTextField(opts, label = { Text("Container Run Options") }, onValueChange = { opts = it })
                Checkbox(opts.contains("--rm"), onCheckedChange = {});Text("--rm")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { runOpts = opts.appSplit();ctrId = cid.ifEmpty { null }; close() }) { Text("Save") }
                    Button(onClick = { close() }) { Text("Cancel") }
                }
            }
        }
        SelectionContainer(Modifier.weight(1f)) { Text(id) }
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
            if (showDialog) pullImgDialog(onClose = { showDialog = false }) { id, opts ->
                running = true
                scope.launch {
                    showDialog = false
                    service.ctr("i", "pull", *(opts.appSplit().toTypedArray()), id)
                    statusOrNull = getStatus()
                    running = false
                }
            }
        }

        // Containers
        if (ctrStatus.containers.isEmpty()) Text("No Container.")
        else ctrStatus.containers.forEach { ctr -> ctr.item() }
        if (errorMessage.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { errorMessage = "" },
                buttons = { Row(Modifier.padding(8.dp)) { Button(onClick = { errorMessage = "" }) { Text("OK") } } },
                title = { Text("Error") },
                text = { Text(errorMessage) })
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
        ) {
            AppPanel()
        }
    }
}

@Composable
fun AppTextField(value: String, label: @Composable() (() -> Unit)? = null, onValueChange: (String) -> Unit) =
    TextField(value = value, label = label, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth())

@Composable
fun pullImgDialog(onClose: () -> Unit, onOk: (id: String, opts: String) -> Unit) = Dialog(onDismissRequest = onClose) {
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

var pullImageId by localStorageString()
var pullImageOpts by localStorageString()

expect fun setStorage(k: String, v: String?)
expect fun getStorage(k: String): String?
class localStorageString(val appId: String = "containerOp") {
    operator fun getValue(n: Nothing?, p: KProperty<*>) = getStorage("$appId.${p.name}")
    operator fun setValue(n: Nothing?, p: KProperty<*>, s: String?) = setStorage("$appId.${p.name}", s)
    operator fun getValue(any: Any, p: KProperty<*>) = getStorage("$appId.${p.name}")
    operator fun setValue(any: Any, p: KProperty<*>, s: String?) = setStorage("$appId.${p.name}", s)
}

inline fun <reified T> T.toJson(): String = Json.encodeToString(this)
inline fun <reified T> String.toObject(): T = Json.decodeFromString(this)

class localStorageList<T>(val appId: String = "containerOp") {
    operator fun getValue(n: Nothing?, p: KProperty<*>): List<T>? = getStorage("$appId.${p.name}")?.toObject()
    operator fun setValue(n: Nothing?, p: KProperty<*>, s: List<T>?) = setStorage("$appId.${p.name}", s?.toJson())
    operator fun getValue(any: Any, p: KProperty<*>): List<T>? = getStorage("$appId.${p.name}")?.toObject()
    operator fun setValue(any: Any, p: KProperty<*>, s: List<T>?) = setStorage("$appId.${p.name}", s?.toJson())
}

fun String?.appSplit() = this?.split(Regex("\\s+"))?.filter { it.isNotEmpty() } ?: listOf()
