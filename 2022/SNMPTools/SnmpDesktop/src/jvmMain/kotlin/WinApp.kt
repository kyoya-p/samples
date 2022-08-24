@file:Suppress("OPT_IN_IS_NOT_ENABLED")

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
import com.charleskorn.kaml.Yaml
import jp.wjg.shokkaa.snmp4jutils.*
import jp.wjg.shokkaa.snmp4jutils.async.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.VariableBinding
import java.awt.FileDialog.LOAD
import java.awt.FileDialog.SAVE
import java.io.File
import java.io.InputStream
import java.io.OutputStream

val currentTime get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
val today get() = currentTime.run { "%04d%02d%02d".format(year, monthNumber, dayOfMonth) }
val now get() = currentTime.run { "%02d%02d%02d".format(hour, minute, second) }
inline fun <reified R> Yaml.decodeFromStream(s: InputStream): R = decodeFromStream(serializersModule.serializer(), s)
inline fun <reified R> Yaml.encodeToStream(v: R, s: OutputStream) = encodeToStream(serializersModule.serializer(), v, s)

@OptIn(ExperimentalSerializationApi::class)
fun saveMib(mib: List<VariableBinding>, path: String) = yamlSnmp4j.encodeToStream(mib, File(path).outputStream())

@OptIn(ExperimentalSerializationApi::class)
fun loadMib(path: String): List<VariableBinding> = yamlSnmp4j.decodeFromStream(File(path).inputStream())

@OptIn(ExperimentalSerializationApi::class, FlowPreview::class)
@ExperimentalCoroutinesApi
@Composable
@Preview
fun WinApp(window: ComposeWindow) = MaterialTheme {
    var logs by remember { mutableStateOf("$now Start Application $today.$now --------------------\n") }
    var mib: List<VariableBinding> by remember {
        val mib = app.mibFile?.let { runCatching { loadMib(it) }.getOrNull() } ?: listOf()
        if (mib.isEmpty()) logs += "$now Error: Empty MIB... Load or Capture Device first.\n"
        else "$now Loaded #${mib.size} Entries.\n"
        mutableStateOf(mib)
    }
    var ipSpec by remember { mutableStateOf(app.ip ?: "") }
    var walking by remember { mutableStateOf(false) }
    val snmpAccessInfoDialog = SnmpCaptureDialog(ipSpec) { ip ->
        ipSpec = ip
        walking = true
    }
    val saveDlg = AwtFileDialog(mode = SAVE) { d, f ->
        saveMib(mib, "$d/$f")
        app.mibFile = "$d/$f"
        logs += "$now Saved MIBS:${mib.size} to $d/$f"
    }
    val loadDlg = AwtFileDialog(mode = LOAD) { d, f ->
        mib = loadMib("$d/$f")
        app.mibFile = "$d/$f"
        logs += "$now Loaded MIBS:${mib.size} from $d/$f"
    }

    // Backgroud Agent task
    LaunchedEffect(mib) {
        logs += "$now Start Agent $today.$now MIBS:${mib.size} ----------\n"
        runCatching {
            snmpAgent(vbl = mib) { ev, pdu ->
                logs += "$now ${ev.peerAddress} > $pdu\n"
                pdu
            }
        }.onFailure { logs += now + it.stackTraceToString() + "\n" }
    }

    // Backgroud SNMP capture task
    if (walking) LaunchedEffect(null) {
        logs += "$now Run Walk IP:$ipSpec ...\n"
        val snmp = SnmpBuilder().udp().v1().build().async()
        val oids = listOf("1.3.6")
        logs += "$now Walking"
        val logs0 = logs
        val r = snmp.walk(ipSpec, oids).flatMapConcat { it.asFlow() }
            .withIndex().onEach { logs = logs0 + " ${it.index}" }.map { it.value }
            .toList()
        logs += "\n$now Cmpl IP:$ipSpec, MIBS:${r.size}\n"
        if (r.isNotEmpty()) mib = r
        walking = false
    }

    Scaffold(topBar = {
        TopAppBar {
            Button(onClick = { snmpAccessInfoDialog.open() }) { Text("Capture Device") }
            Button(onClick = { saveDlg.open() }) { Text("Save") }
            Button(onClick = { loadDlg.open() }) { Text("Load") }
        }
    }) {
        snmpAccessInfoDialog.placing()
        saveDlg.placing()
        loadDlg.placing()
        AutoScrollBox { Text(logs, fontSize = 14.sp, softWrap = false, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
fun AutoScrollBox(contents: @Composable() BoxScope.() -> Unit) {
    val stateVertical = rememberScrollState()
    LaunchedEffect(stateVertical.maxValue) { stateVertical.animateScrollTo(stateVertical.maxValue) }
    Box(modifier = Modifier.fillMaxSize().verticalScroll(stateVertical)) { contents() }
}
