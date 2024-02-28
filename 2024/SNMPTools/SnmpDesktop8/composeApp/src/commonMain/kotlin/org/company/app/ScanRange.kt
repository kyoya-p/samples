import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import jp.wjg.shokkaa.snmp4jutils.async.createDefaultSenderSnmpAsync
import jp.wjg.shokkaa.snmp4jutils.filterResponse
import jp.wjg.shokkaa.snmp4jutils.scanFlow
import jp.wjg.shokkaa.snmp4jutils.toRangeSet
import kotlinx.serialization.Serializable
import moe.tlaster.precompose.PreComposeApp
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.Navigator
import moe.tlaster.precompose.navigation.rememberNavigator
import moe.tlaster.precompose.navigation.transition.NavTransition
import okio.Path.Companion.toPath

expect val userDir: String

@Serializable
data class SnmpDesktop(
    val range: String = "",
    val snmpSettings: SnmpSettings = SnmpSettings(),
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

//    var scanSpec by remember { mutableStateOf("10.36.102.1-10.36.102.254") }
    var app by remember { mutableStateOf(SnmpDesktop()) }
    var loading by remember { mutableStateOf(true) }
    var scanResult by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    LaunchedEffect(scanning) {
        if (scanning) {
            val rangeSet = app.range.toRangeSet()
            scanResult = ""
            createDefaultSenderSnmpAsync().run {
                scanFlow(rangeSet, limit = 500) { ip ->
                    target = defaultSnmpFlowTarget(ip).apply {
                        timeout = 2500
                        retries = 1
                    }
                }.filterResponse().collect {
                    scanResult += "${it.received.peerAddress},\n"
                }
            }
            scanning = false
        }
    }
    @Composable
    fun snmpSettingField() = OutlinedTextField(
        value = app.range,
        onValueChange = { app = app.copy(range = it) },
        label = { Text("IP Range") },
        singleLine = false
    )

    @Composable
    fun resultFiled() = OutlinedTextField(
        value = scanResult.ifEmpty { "No Item" },
        readOnly = true,
        onValueChange = { },
        label = { Text("Result") },
        singleLine = false
    )

    @Composable
    fun runButton() {
        FloatingActionButton(onClick = { scanning = true }) {
            when (scanning) {
                true -> CircularProgressIndicator()
                false -> Icon(Icons.Default.Search, "IP Range Scan")
            }
        }
    }

    println("$userDir/.snmp-desktop.json")
    val store: KStore<SnmpDesktop> = storeOf("$userDir/.snmp-desktop.json".toPath())
    LaunchedEffect(Unit) {
        store.updates.collect {
            if (it != null) app = it
            loading = false
        }
    }
    LaunchedEffect(app) { if (!loading) store.set(app) }


    Scaffold(
//        topBar = { TopAppBar { Text("Scan IP Range") } },
        floatingActionButton = { runButton() }
    ) {
        if (loading) {
            CircularProgressIndicator()
        } else {
            Column(modifier = Modifier.padding(8.dp)) {
                Row {
                    snmpSettingField()
                    IconButton(onClick = { navigator.navigate("/snmpSettings") }) {
                        Icon(
                            Icons.Default.Settings,
                            "SNMP Settings"
                        )
                    }
                }
                resultFiled()
            }
        }
    }
}

@Composable
fun SnmpSettings(navigator: Navigator) {
    var timeout by remember { mutableStateOf("5000") }
    var retries by remember { mutableStateOf("5") }

    @Composable
    fun timeoutField() = OutlinedTextField(
        value = timeout,
        onValueChange = { t -> runCatching { t.toInt() }.onSuccess { timeout = t } },
        label = { Text("Timeout") },
        singleLine = true
    )

    @Composable
    fun retriesField() = OutlinedTextField(
        value = retries,
        onValueChange = { retries = it },
        label = { Text("Retries") },
        singleLine = true
    )


    @Composable
    fun runButton() {
        FloatingActionButton(onClick = { navigator.goBack() }) {
            Icon(Icons.Default.ArrowBack, "Submit")
        }
    }
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

