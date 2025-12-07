import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp

import jp.wjg.shokkaa.snmp4jutils.async.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okio.FileSystem
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.VariableBinding
import java.awt.FileDialog
import java.awt.FileDialog.LOAD
import java.awt.FileDialog.SAVE
import java.io.File

import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.charleskorn.kaml.Yaml
import io.github.xxfast.kstore.KStore
import jp.wjg.shokkaa.snmp4jutils.yamlSnmp4j
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import okio.Path.Companion.toPath
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class WalkerData(
    val ready: Boolean = true, val mibFile: String = "", val ip: String = "192.168.0.1", val commStr: String = "public"
)

@Composable
fun capturePage(window: ComposeWindow) = with(FileSystem.SYSTEM) {
    val appStore: KStore<WalkerData> = dataStore("agent")
    var app by remember { mutableStateOf(WalkerData(ready = false)) }
    LaunchedEffect(Unit) { appStore.updates.collect { app = it ?: WalkerData() } }
    if (!app.ready) return@with

    var snackBarMessage by remember { mutableStateOf("") }
    var mib: List<VariableBinding> by remember { mutableStateOf(listOf()) }
    var filePath by remember { mutableStateOf(app.mibFile) }

    LaunchedEffect(app) { appStore.set(app) }
    LaunchedEffect(mib) {
        snmpAgent(vbl = mib) { ev, pdu ->
            println("$ev: $pdu")
            snackBarMessage="$ev: $pdu"
            pdu
        }
    }
    LaunchedEffect(filePath) { app = app.copy(mibFile = filePath) }

    runCatching { mib = loadMib(filePath.toPath().toFile()) }
    val scope = rememberCoroutineScope()
    val snmpCaptureDialog = snmpCaptureDialog(app) { app1 ->
        scope.launch {
            val msg = { n: Int, v: VariableBinding -> snackBarMessage = "Capturing $n: $v" }
            msg(0, VariableBinding(OID(1, 3, 6)))
            val snmpWalker = SnmpBuilder().udp().v1().build().async().walk(app1.ip, listOf("1.3.6"))
            val walkRes = snmpWalker.withIndex().map { (i, e) -> msg(i, e[0]); e[0] }.toList()
            if (walkRes.isNotEmpty()) mib = walkRes
            snackBarMessage = ""
            app = app1.copy(mibFile = "")
        }
    }

    fun selectFile(opMode: Int) =
        FileDialog(window).apply { mode = opMode; isVisible = true }.takeIf { it.directory != null && it.file != null }
            ?.run { File(directory, file).canonicalFile }?.apply { filePath = path }

    fun loadMib() = selectFile(LOAD)?.let { file -> mib = loadMib(file) }
    fun saveMib() = selectFile(SAVE)?.let { file -> saveMib(mib, file) }

    @Composable
    fun mibFilePathField() = Text(
        (if (filePath.isNotEmpty()) "File: $filePath : \n" else "") + "${mib.size} MIBs in service",
        modifier = Modifier.fillMaxWidth(),
    )
    Scaffold(
        modifier = Modifier.padding(8.dp),
//        snackbarHost = {  },
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            snmpCaptureDialog.placing()
            mibFilePathField()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                Button(onClick = { snmpCaptureDialog.open() }) { Text("Capture") }
                Button(onClick = ::saveMib) { Text("Save") }
                Button(onClick = ::loadMib) { Text("Load") }
            }
            if (snackBarMessage.isNotEmpty()) Snackbar { Text(snackBarMessage, maxLines = 1) }
        }
    }
}

inline fun <reified R> Yaml.decodeFromStream(s: InputStream): R = decodeFromStream(serializersModule.serializer(), s)
inline fun <reified R> Yaml.encodeToStream(v: R, s: OutputStream) = encodeToStream(serializersModule.serializer(), v, s)

@OptIn(ExperimentalSerializationApi::class)
fun saveMib(mib: List<VariableBinding>, file: File) = yamlSnmp4j.encodeToStream(mib, file.outputStream())

@OptIn(ExperimentalSerializationApi::class)
fun loadMib(file: File): List<VariableBinding> = yamlSnmp4j.decodeFromStream(file.inputStream())
