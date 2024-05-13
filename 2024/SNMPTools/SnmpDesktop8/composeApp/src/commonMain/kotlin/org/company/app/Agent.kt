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
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class WalkerData(var mibFile: String? = null, var ip: String = "192.168.0.1", var commStr: String = "public")

@Composable
fun capturePage(window: ComposeWindow) = with(FileSystem.SYSTEM) {
    val appStore: KStore<WalkerData> = dataStore("agent")
    var app0 by remember { mutableStateOf<WalkerData?>(null) }
    LaunchedEffect(Unit) { appStore.updates.collect { app0 = it ?: WalkerData() } }
    if (app0 == null) return@with

    var app by remember { mutableStateOf(app0!!) }
    var snackBarMessage by remember { mutableStateOf("") }
    var mib: List<VariableBinding> by remember { mutableStateOf(listOf()) }
    var filePath by remember { mutableStateOf("") }
    var ipSpec by remember { mutableStateOf(app.ip ?: "") }

    LaunchedEffect(Unit) { appStore.updates.filterNotNull().collect { app = it } }
    LaunchedEffect(app) { appStore.set(app) }
    LaunchedEffect(mib) { snmpAgent(vbl = mib) }


    val scope = rememberCoroutineScope()
    val snmpCaptureDialog = snmpCaptureDialog(app, ipSpec) { ip ->
        ipSpec = ip
        scope.launch {
            val msg = { n: Int -> snackBarMessage = "Capturing MIB [$n]" }
            msg(0)
            val snmpWalker = SnmpBuilder().udp().v1().build().async().walk(ipSpec, listOf("1.3.6"))
            mib = snmpWalker.withIndex().map { (i, e) -> msg(i); e[0] }.toList()
            snackBarMessage = ""
            filePath = ""
        }
    }

    fun selectFile(opMode: Int) = FileDialog(window).apply { mode = opMode; isVisible = true }
        .takeIf { it.directory != null && it.file != null }?.run { File(directory, file).canonicalFile }
        ?.apply { app.mibFile = path }

    fun loadMib() = selectFile(LOAD)?.let { file ->
        mib = loadMib(file)
        filePath = file.absolutePath
    }

    fun saveMib() = selectFile(SAVE)?.let { file ->
        saveMib(mib, file)
        filePath = file.absolutePath
    }


    @Composable
    fun mibFilePathField() = Text(
        (if (filePath.isNotEmpty()) "File: $filePath : \n" else "") + "${mib.size} MIBs in service",
        modifier = Modifier.fillMaxWidth(),
    )
    Scaffold(
        modifier = Modifier.padding(8.dp),
        snackbarHost = { if (snackBarMessage.isNotEmpty()) Snackbar { Text(snackBarMessage) } },
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            snmpCaptureDialog.placing()
            mibFilePathField()
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                Button(onClick = { snmpCaptureDialog.open() }) { Text("Capture") }
                Button(onClick = ::saveMib) { Text("Save") }
                Button(onClick = ::loadMib) { Text("Load") }
            }
        }
    }
}

inline fun <reified R> Yaml.decodeFromStream(s: InputStream): R = decodeFromStream(serializersModule.serializer(), s)
inline fun <reified R> Yaml.encodeToStream(v: R, s: OutputStream) = encodeToStream(serializersModule.serializer(), v, s)

@OptIn(ExperimentalSerializationApi::class)
fun saveMib(mib: List<VariableBinding>, file: File) = yamlSnmp4j.encodeToStream(mib, file.outputStream())

@OptIn(ExperimentalSerializationApi::class)
fun loadMib(file: File): List<VariableBinding> = yamlSnmp4j.decodeFromStream(file.inputStream())
