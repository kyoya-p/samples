import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import jp.wjg.shokkaa.snmp4jutils.*
import jp.wjg.shokkaa.snmp4jutils.async.createDefaultSenderSnmpAsync
import jp.wjg.shokkaa.snmp4jutils.async.toIpV4String
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable
import moe.tlaster.precompose.PreComposeApp
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.Navigator
import moe.tlaster.precompose.navigation.rememberNavigator
import moe.tlaster.precompose.navigation.transition.NavTransition
import okio.Path.Companion.toPath
import kotlin.time.Duration.Companion.milliseconds

expect val userDir: String

@Serializable
data class SnmpDesktop(
    val range: String = "",
    val snmpSettings: SnmpSettings = SnmpSettings(),
    val sessionLimit: Int = 500,
)

@Serializable
data class SnmpSettings(
    val timeout: Int = 5000,
    val retries: Int = 5,
)

@Composable
fun AppNavigator() = PreComposeApp {
    val navigator = rememberNavigator()
    NavHost(
        navigator = navigator,
        navTransition = NavTransition(),
        initialRoute = "/"
    ) {
        scene(route = "/") { ScanRange(navigator) }
        scene(route = "/snmpSettings") { SnmpSettings(navigator) }
//        scene(route = "/detail") { backStackEntry ->
//            backStackEntry.query<String>("url")?.let { url ->
//                SnmpSettings(navigator)
//            }
//        }
    }
}

@Composable
fun ScanRange(navigator: Navigator) {
    var app by remember { mutableStateOf(SnmpDesktop()) }
    var loading by remember { mutableStateOf(true) }
    var scanResult by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    var progressPercent by remember { mutableStateOf(0UL) }
    val store: KStore<SnmpDesktop> = storeOf("$userDir/.snmp-desktop.json".toPath())

    LaunchedEffect(Unit) {
        store.updates.collect {
            if (it != null) app = it
            loading = false
        }
    }
    LaunchedEffect(app) { if (!loading) store.set(app) }

    LaunchedEffect(scanning) {
        if (scanning) {
            runCatching {
                scanResult = ""
                val rangeSet = app.range.toRangeSet()
                var progress = 0UL
                val total = app.range.toRangeSet().totalLength()
                createDefaultSenderSnmpAsync().run {
                    scanFlow(rangeSet, limit = app.sessionLimit) { ip ->
                        target = defaultSnmpFlowTarget(ip).apply {
                            timeout = app.snmpSettings.timeout.toLong()
                            retries = app.snmpSettings.retries
                        }
                    }.onEach { progressPercent = (++progress * 100UL / total) }.filterResponse().collect {
                        scanResult += "${it.received.peerAddress.inetAddress.toIpV4String()},\n"
                    }
                }
            }
            scanning = false
        }
    }

    //    fun UInt.toSiString()=toString().let{it.take(3)+" kMGT"[it.length/3]}
    @Composable
    fun scanRangeField() = OutlinedTextField(
        value = app.range,
        onValueChange = { app = app.copy(range = it) },
        label = { Text("IP Range [${app.range.toRangeSet().totalLength()} addrs]") },
        singleLine = false
    )

    @Composable
    fun sessionLimitFiled() = OutlinedTextField(
        value = app.sessionLimit.toString(),
        onValueChange = { runCatching { app = app.copy(sessionLimit = it.toInt()) } },
        label = { Text("Max Snmp Session [${app.sessionLimit / app.snmpSettings.run { timeout.milliseconds * (retries + 1) }.inWholeSeconds}/sec]") },
        singleLine = false
    )

    @Composable
    fun resultFiled() = OutlinedTextField(
        value = scanResult.ifEmpty { "No Item" },
        readOnly = true,
        onValueChange = { },
        label = { Text("Result [${scanResult.filter { it == '\n' }.count()} found / ${progressPercent}%]") },
        singleLine = false
    )

    @Composable
    fun runButton() {
        FloatingActionButton(onClick = {
            scanning = !scanning
        }) {
            when (scanning) {
                true -> Icon(Icons.Default.Stop, "Stop Scan")
                false -> Icon(Icons.Default.Search, "Start Scan")
            }
        }
    }

    @Composable
    fun snmpSettingsButton() = IconButton(onClick = { navigator.navigate("/snmpSettings") }) {
        Icon(Icons.Default.Settings, "SNMP Settings")
    }

    @Composable
    fun runningIcon() = if (scanning) CircularProgressIndicator(modifier = Modifier.padding(8.dp)) else Unit



    Scaffold(
//        topBar = { TopAppBar { Text("Scan IP Range") } },
        floatingActionButton = { runButton() }
    ) {
        if (loading) {
            CircularProgressIndicator()
        } else {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    scanRangeField()
                    snmpSettingsButton()
                }
                sessionLimitFiled()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    resultFiled()
                    runningIcon()
                }
            }
        }
    }
}

@Composable
fun SnmpSettings(navigator: Navigator) {
    var loading by remember { mutableStateOf(true) }
    var app by remember { mutableStateOf(SnmpDesktop()) }
    val store: KStore<SnmpDesktop> = storeOf("$userDir/.snmp-desktop.json".toPath())
    LaunchedEffect(Unit) {
        store.updates.collect {
            if (it != null) app = it
            loading = false
        }
    }
    LaunchedEffect(app) { if (!loading) store.set(app) }

    @Composable
    fun timeoutField() = OutlinedTextField(
        value = app.snmpSettings.timeout.toString(),
        onValueChange = {
            runCatching { it.toInt() }.onSuccess { app = app.copy(snmpSettings = app.snmpSettings.copy(timeout = it)) }
        },
        label = { Text("Timeout") },
        singleLine = true
    )

    @Composable
    fun retriesField() = OutlinedTextField(
        value = app.snmpSettings.retries.toString(),
        onValueChange = {
            runCatching { it.toInt() }.onSuccess { app = app.copy(snmpSettings = app.snmpSettings.copy(retries = it)) }
        },
        label = { Text("Retries") },
        singleLine = true
    )

    @Composable
    fun runButton() = FloatingActionButton(onClick = { navigator.goBack() }) {
        Icon(Icons.Default.Close, "Submit")
    }

    if (loading) {
        CircularProgressIndicator()
    } else {
        Scaffold(
//        topBar = { TopAppBar { Text("Scan IP Range") } },
            floatingActionButton = { runButton() }
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                timeoutField()
                retriesField()
            }
        }
    }
}

@Composable
inline fun <reified T : @Serializable Any> AppFlow(key: T, loadingSign: () -> Unit, content: () -> Unit) {
    var doc by remember { mutableStateOf(key) }
    var loading = true
    val store: KStore<T> = storeOf("$userDir/.snmp-desktop.json".toPath())
    LaunchedEffect(Unit) {
        store.updates.collect {
            if (it != null) doc = it
            loading = it == null
        }
    }
    if (loading) {
        loadingSign()
    } else {
        content()
    }
}
