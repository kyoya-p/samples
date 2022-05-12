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
import androidx.compose.ui.window.AwtWindow
import jp.wjg.shokkaa.snmp4jutils.mibMapTest
import jp.wjg.shokkaa.snmp4jutils.snmpAgent
import jp.wjg.shokkaa.snmp4jutils.walk
import jp.wjg.shokkaa.snmp4jutils.yamlSnmp4j
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

val currentTime get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
val today get() = currentTime.run { "%04d%02d%02d".format(year, monthNumber, dayOfMonth) }
val now get() = currentTime.run { "%02d%02d%02d".format(hour, minute, second) }

@ExperimentalCoroutinesApi
@Composable
@Preview
fun WinApp(window: ComposeWindow) = MaterialTheme {
    var logs by remember { mutableStateOf("") }
    var mib by remember { mutableStateOf(mibMapTest.values.toList()) }

    val snmpAccessInfoDialog = SnmpAccessInfoDialog { ip ->
        val r = walk(ip).flatMap { it }.onEach { logs += "$now $it\n" }.toList()
        when {
            r.isNotEmpty() -> mib = r
            else -> logs += "$now Could not get MIB from [$ip].\n"
        }
    }
    val saveDialog = AwtFileDialog(mode = FileDialog.SAVE) { dir, res ->
        yamlSnmp4j.encodeToStream(mib, File(dir + "/" + res).outputStream())
    }

    Scaffold(topBar = {
        TopAppBar {
            Button(onClick = { snmpAccessInfoDialog.open() }) { Text("Simulate Device") }
            Button(onClick = { saveDialog.open() }) { Text("Save[TBD]") }
            Button(onClick = { }) { Text("Load[TBD]") }
        }
    }) {
        LaunchedEffect(mib) {
            logs += "\n$now Start $today.$now ------------------\n"
            snmpAgent(vbl = mib) { ev, pdu -> pdu }
        }
        snmpAccessInfoDialog.placing()
        saveDialog.placing()
        AutoScrollBox { Text(logs, fontSize = 14.sp, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
private fun AutoScrollBox(contents: @Composable() BoxScope.() -> Unit) {
    val stateVertical = rememberScrollState()
    LaunchedEffect(stateVertical.maxValue) { stateVertical.animateScrollTo(stateVertical.maxValue) }
    Box(modifier = Modifier.fillMaxSize().verticalScroll(stateVertical)) { contents() }
}

class DialogHandler(val dialogBuilder: @Composable (DialogHandler) -> Unit) {
    var show by mutableStateOf(false)

    @Composable
    fun placing() {
        if (show) dialogBuilder(this)
    }

    fun open() {
        show = true
    }

    fun close() {
        show = false
    }
}

@Composable
private fun AwtFileDialog(
    parent: Frame? = null,
    title: String = "Choose a file",
    mode: Int = FileDialog.LOAD,
    onCloseRequest: (dir: String, file: String) -> Unit,
) = DialogHandler { dialog ->
    AwtWindow(
        create = {
            object : FileDialog(parent, title, mode) {
                override fun setVisible(value: Boolean) {
                    super.setVisible(value)
                    if (value) {
                        if (directory != null && file != null) onCloseRequest(directory, file)
                        dialog.close()
                    }
                }
            }
        },
        dispose = FileDialog::dispose
    )
}
