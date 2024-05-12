import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import jp.wjg.shokkaa.snmp4jutils.async.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okio.FileSystem
import okio.Path.Companion.toPath
import org.snmp4j.PDU
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.VariableBinding
import java.awt.FileDialog
import java.awt.FileDialog.LOAD
import java.awt.FileDialog.SAVE
import java.io.File

import androidx.compose.material3.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun capturePage(window: ComposeWindow) = with(FileSystem.SYSTEM) {
    //
    // Values and Logics
    //
    val setAppTitle = { fileName: String? -> window.title = "${fileName ?: "No File"} - SNMP Desktop" }
//    val snackbarHostState = remember { SnackbarHostState() }
//    var showSnackbar by remember { mutableStateOf(true) }
    var snackBarMessage by remember { mutableStateOf("") }

    var mib: List<VariableBinding> by remember {
        val mib = app.mibFile?.let { runCatching { loadMib(File(it)) }.getOrNull() } ?: listOf()
        setAppTitle(app.mibFile)
        mutableStateOf(mib)
    }
    var ipSpec by remember { mutableStateOf(app.ip ?: "") }

    val scope = rememberCoroutineScope()
    val snmpCaptureDialog = snmpCaptureDialog(app, ipSpec) { ip ->
        ipSpec = ip
        scope.launch {
            launch {
                snackBarMessage = "Capturing MIB [0]"
            }
            //             logs += "$now Walking IP:$ipSpec ...\n"
            val snmp = SnmpBuilder().udp().v1().build().async()
            val oids = listOf("1.3.6")
            val r = snmp.walk(ipSpec, oids).map { it[0] }.toList()
//            logs += "$now Completed. IP:$ipSpec, MIBS:${r.size}\n"
//            logs.lastLines.forEach { println(it) }
            if (r.isNotEmpty()) mib = r
            snackBarMessage = ""
        }
    }

    fun selectFile(opMode: Int) =
        FileDialog(window).apply { mode = opMode; isVisible = true }.takeIf { it.directory != null && it.file != null }
            ?.run { File(directory, file).canonicalFile }?.apply { app.mibFile = path; setAppTitle(path) }

    fun loadMib() = selectFile(LOAD)?.let { file ->
        mib = loadMib(file)
    }

    fun saveMib() = selectFile(SAVE)?.let { file ->
        saveMib(mib, file)
    }

    fun appTitle() = @Composable { Text("SNMP Desktop", maxLines = 1, overflow = TextOverflow.Clip) }

    var filePath by remember { mutableStateOf("") }
    LaunchedEffect(filePath) {
        runCatching {
            mib = loadMib(filePath.toPath().toFile())
        }.onFailure { mib = listOf() }
    }

    LaunchedEffect(mib) {
        runCatching {
            snmpAgent(vbl = mib) { ev, pdu ->
                val pduTypes = mapOf(PDU.GET to "GT", PDU.GETNEXT to "GN", PDU.GETBULK to "BK")
                pdu
            }
        }//.onFailure { logs += "$now ${it.message}\n" }
    }

    //
    // UI Components
    //
    @Composable
    fun mibFilePathField() =
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = filePath,
            label = { Text("MIB File [mibs:${mib.size}]") },
            onValueChange = { filePath = it }
        )

    Scaffold(
        modifier = Modifier.padding(8.dp),
//        topBar = { TopAppBar(title = appTitle(), actions = {
//                Button(onClick = { snmpCaptureDialog.open() }) { Text("Capture") }
//                Button(onClick = ::saveMib) { Text("Save") }
//                Button(onClick = ::loadMib) { Text("Load") }
//            })
//        },
        snackbarHost = {
            if (snackBarMessage.isNotEmpty()) {
                 Snackbar { Text(snackBarMessage) }
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            snmpCaptureDialog.placing()
            mibFilePathField()
            Button(onClick = { snmpCaptureDialog.open() }) { Text("Capture") }
            Button(onClick = ::saveMib) { Text("Save") }
            Button(onClick = ::loadMib) { Text("Load") }

//        autoScrollBox {
//            var log by remember { mutableStateOf("") }
//            LaunchedEffect(Unit) {
//                while (true) {
//                    delay(200)
////                    log = logs.lastLines.joinToString("")
//                }
//            }
//
//            SelectionContainer {
//                Text(log, fontSize = 13.sp, softWrap = false, modifier = Modifier.fillMaxSize())
//            }
//        }
        }
    }
}
