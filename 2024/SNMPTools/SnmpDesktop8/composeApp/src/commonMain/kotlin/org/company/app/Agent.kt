import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp

import jp.wjg.shokkaa.snmp4jutils.async.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okio.FileSystem
import okio.Path.Companion.toPath
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.VariableBinding
import java.awt.FileDialog
import java.awt.FileDialog.LOAD
import java.awt.FileDialog.SAVE
import java.io.File

import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import jp.wjg.shokkaa.snmp4jutils.yamlSnmp4j
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
fun saveMib(mib: List<VariableBinding>, file: File) = yamlSnmp4j.encodeToStream(mib, file.outputStream())

@OptIn(ExperimentalSerializationApi::class)
fun loadMib(file: File): List<VariableBinding> = yamlSnmp4j.decodeFromStream(file.inputStream())

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
    var filePath by remember { mutableStateOf("") }
    LaunchedEffect(filePath) {
        mib = runCatching { loadMib(filePath.toPath().toFile()) }.getOrElse { listOf() }
    }
    var ipSpec by remember { mutableStateOf(app.ip ?: "") }
    val scope = rememberCoroutineScope()
    val snmpCaptureDialog = snmpCaptureDialog(app, ipSpec) { ip ->
        ipSpec = ip
        scope.launch {
            fun msg(n: Int) {
                snackBarMessage = "Capturing MIB [$n]"
            }
            launch {
                msg(0)
            }
            //             logs += "$now Walking IP:$ipSpec ...\n"
            val snmp = SnmpBuilder().udp().v1().build().async()
            val oids = listOf("1.3.6")
            mib = snmp.walk(ipSpec, oids).withIndex().map { (i, e) -> msg(i); e[0] }
                .toList()
            snackBarMessage = ""
        }
    }

    fun selectFile(opMode: Int) =
        FileDialog(window).apply { mode = opMode; isVisible = true }.takeIf { it.directory != null && it.file != null }
            ?.run { File(directory, file).canonicalFile }?.apply { app.mibFile = path; setAppTitle(path) }

    fun loadMib() = selectFile(LOAD)?.let { file ->
        mib = loadMib(file)
        filePath = file.absolutePath
    }

    fun saveMib() = selectFile(SAVE)?.let { file ->
        saveMib(mib, file)
        filePath = file.absolutePath
    }

    LaunchedEffect(mib) {
        snmpAgent(vbl = mib)
    }

    //
    // UI Components
    //
    @Composable
    fun mibFilePathFieldX() = OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = filePath,
        label = { Text("MIB File [mibs:${mib.size}]") },
        onValueChange = { filePath = it }
    )

    @Composable
    fun mibFilePathField() = Text(
        if (filePath.isNotEmpty()) "File: $filePath : " else ""
                + if (mib.isNotEmpty()) "${mib.size} MIBs" else "No MIBs",
        modifier = Modifier.fillMaxWidth(),
    )

    Scaffold(
        modifier = Modifier.padding(8.dp),
        snackbarHost = { if (snackBarMessage.isNotEmpty()) Snackbar { Text(snackBarMessage) } },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            snmpCaptureDialog.placing()
            mibFilePathField()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                Button(onClick = { snmpCaptureDialog.open() }) { Text("Capture") }
                Button(onClick = ::saveMib) { Text("Save") }
                Button(onClick = ::loadMib) { Text("Load") }
            }
        }
    }
}
