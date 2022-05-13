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
import com.charleskorn.kaml.Yaml
import jp.wjg.shokkaa.snmp4jutils.mibMapTest
import jp.wjg.shokkaa.snmp4jutils.snmpAgent
import jp.wjg.shokkaa.snmp4jutils.walk
import jp.wjg.shokkaa.snmp4jutils.yamlSnmp4j
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.InputStream
import java.io.OutputStream

val currentTime get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
val today get() = currentTime.run { "%04d%02d%02d".format(year, monthNumber, dayOfMonth) }
val now get() = currentTime.run { "%02d%02d%02d".format(hour, minute, second) }
inline fun <reified R> Yaml.decodeFromStream(s: InputStream): R = decodeFromStream(serializersModule.serializer(), s)
inline fun <reified R> Yaml.encodeToStream(v: R, s: OutputStream) = encodeToStream(serializersModule.serializer(), v, s)

@OptIn(ExperimentalSerializationApi::class)
@ExperimentalCoroutinesApi
@Composable
@Preview
fun WinApp(window: ComposeWindow) = MaterialTheme {
    var logs by remember { mutableStateOf("$now Start $today.$now --------------------\n") }
    var mib by remember { mutableStateOf(mibMapTest.values.toList()) }

    val snmpAccessInfoDialog = SnmpAccessInfoDialog { ip ->
        val r = walk(ip).flatMap { it }.onEach { logs += "$now $it\n" }.toList()
        when {
            r.isNotEmpty() -> mib = r
            else -> logs += "$now Could not get MIB from [$ip].\n"
        }
    }
    val saveDialog = AwtFileDialog(mode = FileDialog.SAVE) { dir, res ->
        yamlSnmp4j.encodeToStream(mib, File("$dir/$res").outputStream())
    }
    val loadDialog = AwtFileDialog(mode = FileDialog.LOAD) { dir, res ->
        mib = yamlSnmp4j.decodeFromStream(File("$dir/$res").inputStream())
    }

    LaunchedEffect(mib) {
        logs += "$now Start Agent $today.$now ----------\n"
        snmpAgent(vbl = mib) { ev, pdu ->
            logs += "$now ${ev.peerAddress} > $pdu\n"
            pdu
        }
    }

    Scaffold(topBar = {
        TopAppBar {
            Button(onClick = { snmpAccessInfoDialog.open() }) { Text("Simulate Device") }
            Button(onClick = { saveDialog.open() }) { Text("Save") }
            Button(onClick = { loadDialog.open() }) { Text("Load") }
        }
    }) {
        snmpAccessInfoDialog.placing()
        saveDialog.placing()
        loadDialog.placing()
        AutoScrollBox { Text(logs, fontSize = 14.sp, softWrap = false, modifier = Modifier.fillMaxSize()) }
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
            }.apply {
                setFilenameFilter { dir, name -> File("$dir/$name").extension == "yaml" }
            }
        },
        dispose = FileDialog::dispose
    )
}
