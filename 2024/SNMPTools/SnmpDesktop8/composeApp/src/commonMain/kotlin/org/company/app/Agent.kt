import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import jp.wjg.shokkaa.snmp4jutils.async.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.snmp4j.PDU
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.VariableBinding
import java.awt.FileDialog
import java.awt.FileDialog.LOAD
import java.awt.FileDialog.SAVE
import java.io.File

@Composable
fun capturePage(window: ComposeWindow) {
    val setAppTitle = { fileName: String? -> window.title = "${fileName ?: "No File"} - SNMP Desktop" }

    var mib: List<VariableBinding> by remember {
        val mib = app.mibFile?.let { runCatching { loadMib(File(it)) }.getOrNull() } ?: listOf()
        setAppTitle(app.mibFile)
        mutableStateOf(mib)
    }
    var ipSpec by remember { mutableStateOf(app.ip ?: "") }
    val snmpCaptureDialog = snmpCaptureDialog(app, ipSpec) { ip ->
        ipSpec = ip
        GlobalScope.launch {
//            logs += "$now Walking IP:$ipSpec ...\n"
            val snmp = SnmpBuilder().udp().v1().build().async()
            val oids = listOf("1.3.6")
            val r = snmp.walk(ipSpec, oids).map { it[0] }.toList()
//            logs += "$now Completed. IP:$ipSpec, MIBS:${r.size}\n"
//            logs.lastLines.forEach { println(it) }
            if (r.isNotEmpty()) mib = r
        }
    }

    fun selectFile(opMode: Int) =
        FileDialog(window).apply { mode = opMode; isVisible = true }.takeIf { it.directory != null && it.file != null }
            ?.run { File(directory, file).canonicalFile }?.apply { app.mibFile = path; setAppTitle(path) }

    fun loadMib() = selectFile(LOAD)?.let { file ->
        mib = loadMib(file)
//        logs += "$now Loaded MIBS:${mib.size} from ${file.path}\n"
    }

    fun saveMib() = selectFile(SAVE)?.let { file ->
        saveMib(mib, file)
//        logs += "$now Saved MIBS:${mib.size} to ${file.path}\n"
    }

    // Backgroud Agent task
    LaunchedEffect(mib) {
//        logs += "$now Start Agent $today.$now MIBS:${mib.size} ----------\n"
        runCatching {
            snmpAgent(vbl = mib) { ev, pdu ->
                val pduTypes = mapOf(PDU.GET to "GT", PDU.GETNEXT to "GN", PDU.GETBULK to "BK")
//                logs += "$now ${ev.peerAddress}:${pduTypes[ev.pdu.type] ?: ev.pdu.type} > ES:${pdu.errorStatus} EI:${pdu.errorIndex} VBs:${pdu.variableBindings}\n"
                pdu
            }
        }//.onFailure { logs += "$now ${it.message}\n" }
    }

    fun appTitle() = @Composable { Text("SNMP Desktop", maxLines = 1, overflow = TextOverflow.Clip) }



    Scaffold(topBar = {
        TopAppBar(title = appTitle(), actions = {
            Button(onClick = { snmpCaptureDialog.open() }) { Text("Capture") }
            Button(onClick = ::saveMib) { Text("Save") }
            Button(onClick = ::loadMib) { Text("Load") }
        })
    }) {
        snmpCaptureDialog.placing()
        Column{

        }
        autoScrollBox {
            var log by remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(200)
//                    log = logs.lastLines.joinToString("")
                }
            }

            SelectionContainer {
                Text(log, fontSize = 13.sp, softWrap = false, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
