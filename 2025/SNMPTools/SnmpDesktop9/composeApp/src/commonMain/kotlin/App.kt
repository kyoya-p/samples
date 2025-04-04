import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import io.ktor.client.*
import io.ktor.http.*
import jp.wjg.shokkaa.container.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock.System.now
import kotlinx.rpc.krpc.ktor.client.installRPC
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.krpc.streamScoped
import kotlinx.rpc.withService
import kotlin.time.Duration.Companion.seconds

expect val DEV_SERVER_HOST: String

val client by lazy { HttpClient { installRPC() } }

@Composable
fun App() {

    class CtImage(val id: String)
    class CtContainer(val id: String, val imgId: String)
    class CtTask(val execId: String, val pId: String, val status: String)
    class CtProcess(val pId: String, val execId: String?)
    class CtStatus(val images: List<CtImage>, val containers: List<CtContainer>, val tasks: Map<String, CtTask>)

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

    fun <T> List<String>.mkItems(op: (List<String>) -> T) = drop(1).map { it.split(Regex("\\s+")) }.map(op)
    suspend fun getStatus() = CtStatus(
        images = service.ctr("i", "ls", "-q").stdout.map { CtImage(it.trim()) },
        containers = service.ctr("c", "ls").stdout.mkItems { CtContainer(it[0], it[1]) },
        tasks = service.ctr("t", "ls").stdout.mkItems { CtTask(it[0], it[1], it[2]) }.associate { it.execId to it }
    )

    LaunchedEffect(Unit) { streamScoped { statusOrNull = getStatus() } }

    val ctrStatus = statusOrNull ?: return CircularProgressIndicator(color = Color.Blue)
    suspend fun CtImage.runContainer(ctrId: String, vararg args: String) = service.ctr("run", "-d", id, ctrId, *args)
    suspend fun CtImage.remove() = service.ctr("i", "rm", id)
    suspend fun CtContainer.start(ctrId: String) = service.ctr("run", id, ctrId)
    suspend fun CtContainer.remove() = service.ctr("c", "rm", id)
    suspend fun CtContainer.start() = service.ctr("t", "start", "-d", id)
    suspend fun CtContainer.listProcess() = service.ctr("t", "ps", id).stdout.mkItems { CtProcess(it[0], it[1]) }
    suspend fun CtContainer.killTask(signal: Int = 9) = service.ctr("t", "kill", "-s", "$signal", id)
    suspend fun CtContainer.removeTask() = service.ctr("t", "rm", id)
    suspend fun CtTask.kill(signal: Int = 9) = service.ctr("t", "kill", "-s", "$signal", execId)
    suspend fun CtTask.remove() = service.ctr("t", "rm", execId)


    @Composable
    fun AppRow(content: @Composable RowScope.() -> Unit) = Card {
        SelectionContainer {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
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
        CtrActionButton(Icons.Default.PlayArrow) { runContainer("C-${now().toEpochMilliseconds() % 1000}") }
        Text(id, Modifier.weight(1f))
        CtrActionButton(Icons.Default.Delete) { remove() }
    }

    @Composable
    fun CtContainer.item() = AppRow {
        var ps by remember { mutableStateOf(emptyList<CtProcess>()) }
        LaunchedEffect(Unit) {
            while (true) {
                ps = listProcess(); delay(5.seconds)
            }
        }
        Icon(Icons.Default.Star, "")
        Text(id, Modifier.weight(.3f))
        Text(imgId)
        Row(verticalAlignment = Alignment.CenterVertically) {
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
        if (ctrStatus.images.isEmpty()) Text("No Images.")
        else ctrStatus.images.forEach { it.item() }
        IconButton(onClick = { showDialog = true }) {
            Icon(Icons.Default.Add, "Pull Image")
            Icon(Icons.Default.Add, "Pull Image")
            if (running) CircularProgressIndicator()
        }

        if (ctrStatus.containers.isEmpty()) Text("No Container.")
        else ctrStatus.containers.forEach { ctr ->
            ctr.item()
        }

        if (errorMessage.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { errorMessage = "" },
                buttons = { Button(onClick = { errorMessage = "" }) { Text("OK") } },
                title = { Text("Error") },
                text = { Text(errorMessage) })
        }

        SnmpRequestField(service)
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
fun SnmpRequestField(service: UserService) {
    val state = rememberTextFieldState("")
//    BasicTextField(
//        state,
//        textStyle = TextStyle(fontSize = 16.sp),
//        modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray, RoundedCornerShape(6.dp)).padding(8.dp),
////        onValueChange = { state = it },
//    )

    Row {
        var udpAdr by remember { mutableStateOf("") }
        TextField(udpAdr, label = { Text("Address") }, onValueChange = { udpAdr = it })
        ActionButton(Icons.Filled.PlayArrow) {
            val (adr, p) = udpAdr.split(":")
            val req = SnmpRequest(
                target = SnmpTargetV1(adr, p.toInt(), "public", 5, 5000),
                pduType = SnmpRequest.GETNEXT,
                vbl = listOf(SnmpVarBind(".1.3.6"))
            )
            service.unicast(req)
        }
    }
}

@Composable
fun ActionButton(face: ImageVector, op: suspend () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    IconButton(onClick = {
        busy = true
        scope.launch {
            op()
            busy = false
        }
    }) { Icon(face, "") }
    if (busy) CircularProgressIndicator()
}
