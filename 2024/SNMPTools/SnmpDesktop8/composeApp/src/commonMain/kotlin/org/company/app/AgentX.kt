//package org.company.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.charleskorn.kaml.Yaml
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.serializer
import java.io.InputStream
import java.io.OutputStream

//val currentTime get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
//val today get() = currentTime.run { "%04d%02d%02d".format(year, monthNumber, dayOfMonth) }
//val now get() = currentTime.run { "%02d%02d%02d.%03d".format(hour, minute, second, nanosecond / 1000_000) }
//inline fun <reified R> Yaml.decodeFromStream(s: InputStream): R = decodeFromStream(serializersModule.serializer(), s)
//inline fun <reified R> Yaml.encodeToStream(v: R, s: OutputStream) = encodeToStream(serializersModule.serializer(), v, s)


//@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
//@Composable
//fun agentPage(window: ComposeWindow) {
//    val setAppTitle = { fileName: String? -> window.title = "${fileName ?: "No File"} - SNMP Desktop" }
//
//    var mib: List<VariableBinding> by remember {
//        val mib = app.mibFile?.let { runCatching { loadMib(File(it)) }.getOrNull() } ?: listOf()
//        setAppTitle(app.mibFile)
//        mutableStateOf(mib)
//    }
//    var ipSpec by remember { mutableStateOf(app.ip ?: "") }
//    val snmpCaptureDialog = snmpCaptureDialog(app, ipSpec) { ip ->
//        ipSpec = ip
//        GlobalScope.launch {
////            logs += "$now Walking IP:$ipSpec ...\n"
//            val snmp = SnmpBuilder().udp().v1().build().async()
//            val oids = listOf("1.3.6")
//            val r = snmp.walk(ipSpec, oids).map { it[0] }.toList()
////            logs += "$now Completed. IP:$ipSpec, MIBS:${r.size}\n"
////            logs.lastLines.forEach { println(it) }
//            if (r.isNotEmpty()) mib = r
//        }
//    }
//
//    fun selectFile(opMode: Int) = FileDialog(window).apply { mode = opMode; isVisible = true }
//        .takeIf { it.directory != null && it.file != null }
//        ?.run { File(directory, file).canonicalFile }?.apply { app.mibFile = path; setAppTitle(path) }
//
//    fun loadMib() = selectFile(LOAD)?.let { file ->
//        mib = loadMib(file)
////        logs += "$now Loaded MIBS:${mib.size} from ${file.path}\n"
//    }
//
//    fun saveMib() = selectFile(SAVE)?.let { file ->
//        saveMib(mib, file)
////        logs += "$now Saved MIBS:${mib.size} to ${file.path}\n"
//    }
//
//    // Backgroud Agent task
//    LaunchedEffect(mib) {
////        logs += "$now Start Agent $today.$now MIBS:${mib.size} ----------\n"
//        runCatching {
//            snmpAgent(vbl = mib) { ev, pdu ->
//                val pduTypes = mapOf(PDU.GET to "GT", PDU.GETNEXT to "GN", PDU.GETBULK to "BK")
////                logs += "$now ${ev.peerAddress}:${pduTypes[ev.pdu.type] ?: ev.pdu.type} > ES:${pdu.errorStatus} EI:${pdu.errorIndex} VBs:${pdu.variableBindings}\n"
//                pdu
//            }
//        }//.onFailure { logs += "$now ${it.message}\n" }
//    }
//
//    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
//    Scaffold(
//        topBar = {
//            CenterAlignedTopAppBar(
//                navigationIcon = {
//                    IconButton(onClick = { /* do something */ }) {
//                        Icon(
//                             Icons.Filled.ArrowBack,
//                            contentDescription = "Localized description"
//                        )
//                    }
//                },
//                title = { Text("AAA") },
//                actions = {
//
//                    Button(onClick = { snmpCaptureDialog.open() }) { Text("Capture") }
//                    Button(onClick = ::saveMib) { Text("Save") }
//                    Button(onClick = ::loadMib) { Text("Load") }
//                }
//            )
//        })
//    {
//        snmpCaptureDialog.placing()
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
//    }
//}

//@Composable
//fun autoScrollBox(contents: @Composable BoxScope.() -> Unit) {
//    val stateVertical = rememberScrollState()
//    LaunchedEffect(stateVertical.maxValue) { stateVertical.animateScrollTo(stateVertical.maxValue) }
//    Box(modifier = Modifier.fillMaxSize().verticalScroll(stateVertical)) {
//        contents()
//    }
//}
