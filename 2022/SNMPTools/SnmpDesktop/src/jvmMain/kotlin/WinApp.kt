import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.sp
import jp.wjg.shokkaa.snmp4jutils.mibMapTest
import jp.wjg.shokkaa.snmp4jutils.snmpAgent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Suppress("unused")
fun currentTime() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
fun today() = currentTime().run { "%04d%02d%02d".format(year, monthNumber, dayOfMonth) }
fun now() = currentTime().run { "%02d%02d%02d".format(hour, minute, second) }

//val agentList = mutableListOf<SnmpAgent>()

@ExperimentalCoroutinesApi
@Composable
@Preview
fun WinApp(window: ComposeWindow) = MaterialTheme {
    val isRunning by remember { mutableStateOf(true) }
    var isActiveCloningDialog by remember { mutableStateOf(false) }
    var isActiveOpenDialog by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf("\n") }

    Scaffold(topBar = {
        TopAppBar {
            Button(onClick = { isActiveCloningDialog = true }) { Text("Simulate") }
        }
    }) {
        LaunchedEffect(isRunning) {
            while (isRunning) {
                logs += "${today()} Start ------------------\n"
                snmpAgent(vbl = mibMapTest.values.toList()) { ev, pdu ->
                    logs += "${now()} ${ev.peerAddress} ${pdu.variableBindings}\n"
                    pdu
                }
                delay(500)
            }
        }
        AutoScrollBox {
            Text(logs, fontSize = 14.sp, modifier = Modifier.fillMaxSize())
        }

        if (isActiveOpenDialog) {
            FileDialog(window = window, title = "Notepad", isLoad = true) {
                isActiveOpenDialog = false
                println(it)
            }
        }

        if (isActiveCloningDialog) SettingsDialog(onClose = { isActiveCloningDialog = false }) {

        }
    }
}

@Composable
private fun AutoScrollBox(contents: @Composable() BoxScope.() -> Unit) {
    val stateVertical = rememberScrollState()
    LaunchedEffect(stateVertical.maxValue) { stateVertical.animateScrollTo(stateVertical.maxValue) }
    Box(
        modifier = Modifier.fillMaxSize().verticalScroll(stateVertical)
    ) {
        contents()
    }
}

