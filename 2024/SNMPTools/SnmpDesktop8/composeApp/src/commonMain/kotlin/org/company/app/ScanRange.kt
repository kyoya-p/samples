import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import jp.wjg.shokkaa.snmp4jutils.async.createDefaultSenderSnmpAsync
import jp.wjg.shokkaa.snmp4jutils.async.defaultPDU
import jp.wjg.shokkaa.snmp4jutils.async.toIpV4String
import jp.wjg.shokkaa.snmp4jutils.filterResponse
import jp.wjg.shokkaa.snmp4jutils.scanFlow
import jp.wjg.shokkaa.snmp4jutils.toRangeSet
import jp.wjg.shokkaa.snmp4jutils.totalLength
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable
import moe.tlaster.precompose.navigation.Navigator
import okio.Path.Companion.toPath
import kotlin.time.Duration.Companion.milliseconds


@Serializable
data class SnmpDesktop(
    val scanRange: String = "",
    val ignoreRange: String = "",
    val snmpSettings: SnmpSettings = SnmpSettings(),
    val sessionLimit: Int = 5000,
)

@Serializable
data class SnmpSettings(
    val timeout: Int = 2500,
    val retries: Int = 1,
)

//@Composable
//fun AppNavigator() = PreComposeApp {
//    val navigator = rememberNavigator()
//    NavHost(
//        navigator = navigator,
//        navTransition = NavTransition(),
//        initialRoute = "/"
//    ) {
//        scene(route = "/") { ScanRange(navigator) }
//        scene(route = "/snmpSettings") { SnmpSettings(navigator) }
////        scene(route = "/detail") { backStackEntry ->
////            backStackEntry.query<String>("url")?.let { url ->
////                SnmpSettings(navigator)
////            }
////        }
//    }
//}

@Composable
fun scannerPage() {
    var app by remember { mutableStateOf(SnmpDesktop()) }
    var loading by remember { mutableStateOf(true) }
    var scanResult by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    var progressPercent by remember { mutableStateOf(0UL) }
    val store: KStore<SnmpDesktop> = storeOf("$userDir/.snmp-desktop/scanner.json".toPath())

    LaunchedEffect(Unit) {
        store.updates.collect {
            if (it != null) app = it
            loading = false
        }
    }
    LaunchedEffect(app) { if (!loading) store.set(app) }

    val totalRange = app.scanRange.toRangeSet().apply { removeAll(app.ignoreRange.toRangeSet()) }

    @Composable
    fun scanRangeField() = OutlinedTextField(
        value = app.scanRange,
        onValueChange = { app = app.copy(scanRange = it) },
        label = { Text("IP Range [${totalRange.totalLength()} addrs / ${app.scanRange.toRangeSet().totalLength()} ]") },
        singleLine = false
    )

    @Composable
    fun ignoreRangeField() = OutlinedTextField(
        value = app.ignoreRange,
        onValueChange = { app = app.copy(ignoreRange = it) },
        label = { Text("Ignore IP Range [${app.ignoreRange.toRangeSet().totalLength()} addrs]") },
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
    fun runButton() = if (totalRange.totalLength() > 0UL || scanning) {
        FloatingActionButton(onClick = { scanning = !scanning }) {
            when {
                scanning -> Icon(Icons.Default.Stop, "Stop Scan")
                else -> Icon(Icons.Default.Search, "Start Scan")
            }
        }
    } else Unit
    LaunchedEffect(scanning) {
        if (scanning) {
            runCatching {
                scanResult = ""
                var progress = 0UL
                val total = totalRange.totalLength()
                createDefaultSenderSnmpAsync().run {
                    scanFlow(totalRange, maxSessions = app.sessionLimit) {
                        target = defaultSnmpFlowTarget(ip).apply {
                            timeout = app.snmpSettings.timeout.toLong()
                            retries = app.snmpSettings.retries
                        }
                        pdu = defaultPDU()
                    }.onEach { progressPercent = (++progress * 100UL / total) }.filterResponse().collect {
                        val adr = it.received.peerAddress.inetAddress.toIpV4String()
                        val v = it.received.response[0].variable.toString()
                        scanResult += "{$adr: $v},\n"
                    }
                }
            }.onFailure { it.printStackTrace() }
            scanning = false
        }
    }

    @Composable
    fun snmpSettingsButton() = IconButton(onClick = {
//        navigator.navigate("/snmpSettings")

    }) {
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
                ignoreRangeField()
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
