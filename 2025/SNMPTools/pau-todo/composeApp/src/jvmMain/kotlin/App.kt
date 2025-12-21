@file:OptIn(ExperimentalSerializationApi::class)

package jp.wjg.shokkaa.snmp

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.random.Random
import kotlin.time.Clock.System.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
fun App() = MaterialTheme {
    var app by remember { mutableStateOf(config) }
    Scaffold { app.Main { app = it; config = app } }
}


// type PageModeに応じた数値定数定義(PageMode.DEVLIST=0, PageMode.METRICS=1)
enum class PageMode { DEVLIST, METRICS, TIMECHART }

@Serializable
data class Log(
    @ProtoNumber(1) val n: Int, // 要求連番
    @ProtoNumber(2) val t0: Int, // 要求生成時刻
    @ProtoNumber(3) val t1: Int, // 送信時刻
    @ProtoNumber(4) val t2: Int, // コールバック時刻
)

data class Metrics(
    val histgram: ArrayDeque<Int> = ArrayDeque(),
    val logs: ArrayDeque<Log>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppData.Main(
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    scope: CoroutineScope = rememberCoroutineScope(),
    mode: MutableState<PageMode> = remember { mutableStateOf(PageMode.DEVLIST) },
    onChange: (AppData) -> Unit
) = ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
        fun close() = scope.launch { drawerState.close() }

        @Composable
        fun Item(label: String, id: PageMode, onClick: () -> Unit = { mode.value = id; close() }) =
            NavigationDrawerItem(
                label = { Text(text = label) },
                selected = mode.value == id,
                onClick = onClick,
            )
        ModalDrawerSheet {
            Text("SNMP Scanner", modifier = Modifier.padding(16.dp))
            HorizontalDivider()
            Item("\uD83D\uDDA8\uFE0FDevices", PageMode.DEVLIST)
            Item("📊Metrics", PageMode.METRICS)
            Item("📊TImeChart", PageMode.TIMECHART)
            Item("⚙Settings", mode.value, onClick = SettingDialog { onChange(it) }.also { close() })
        }
    }
) {
    Column {
        var log = Metrics(
            histgram = ArrayDeque(0),
            logs = ArrayDeque(0)
        )
        val globalSnmpThrottle = key(scanRate) { rateLimiter() }
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Filled.PlayArrow, "drawer")
                }
            },
            title = { Text("PAU", fontWeight = FontWeight.Bold, overflow = TextOverflow.Clip) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            actions = {
                MfpAddField(globalSnmpThrottle) { onChange(it) }
                IconButton(onClick = { onChange(copy(mfps = emptyMap())) }) { Icon(Icons.Default.Delete, "delete") }
                IconButton(onClick = SettingDialog { onChange(it) }) { Icon(Icons.Default.Settings, "settings") }
            },
        )
        when (mode.value) {
            PageMode.DEVLIST -> DevList { onChange(it) }
//            PageMode.METRICS -> SendLogGraph(log)
            PageMode.METRICS -> FibonacciBarPlot()
            PageMode.TIMECHART -> DynamicTimeSeriesChart()
        }
    }
}

@Composable
fun AppData.DevList(onChange: (AppData) -> Unit) {
    Row(Modifier.padding(4.dp).fillMaxWidth()) {
        val state = rememberScrollState()
        key(mfps, scanRate) {
            Column(Modifier.verticalScroll(state).weight(1f)) {
                //LazyColumn不可。LazyColumnは画面表示中のMFPのみインスタン化するため非表示の機器は情報採取されない。
                mfps.entries.forEach { (_, mfp) ->
                    mfp.StatusRow(rateLimiter(), updateInterval = updateInterval.seconds, onRemove = { ip ->
                        onChange(copy(mfps = mfps.toMutableMap().apply { remove(ip) }))
                    })
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterVertically).width(16.dp),
            adapter = rememberScrollbarAdapter(state),
        )
    }
}

@Composable
fun AppData.Settings(
//    appData: MutableState<AppData> = remember { mutableStateOf(this) },
    onChange: (AppData) -> Unit
) {
    var app by remember { mutableStateOf(this) }

    @Composable
    fun SnmpConfig.SnmpConfigField(onChange: (SnmpConfig) -> Unit) = Column {
        val packets = retries + 1
        val maxTO = intervalMS.milliseconds * packets
        IntField(retries, "Retries [0~](max $packets packets / req)") { onChange(copy(retries = it)) }
        IntField(intervalMS, "Timeout[msec] (max $maxTO / req)") { onChange(copy(intervalMS = it)) }
        TextField(
            commStrV1, label = { Text("Community String") },
            onValueChange = { onChange(copy(commStrV1 = it)) })
    }

    @Composable
    fun ScanRateField() {
        val status =
            runCatching { app.scanRate().let { "Scan Rate ( ${it[0]} req / ${it[1]} msec )" } }.getOrNull()
        TextField(
            value = app.scanRate,
            onValueChange = { app = app.copy(scanRate = it) },
            label = { Text(status ?: "-") },
            isError = status == null,
            singleLine = true,
        )
    }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        app.scanSnmp.SnmpConfigField { app = app.copy(scanSnmp = it) }
        IntField(app.updateInterval, "Update Interval[sec] (0=no update)") { app = app.copy(updateInterval = it) }
        ScanRateField()
        IntField(app.scanScrambleBlock, "Scan Scramble Block[0-16]") { app = app.copy(scanScrambleBlock = it) }
        IntField(app.receiveBufferSize, "Receive Buffer Size[Byte]") { app = app.copy(receiveBufferSize = it) }
        onChange(app)
    }
}

@Composable
fun AppData.SettingDialog(
    appData: MutableState<AppData> = remember { mutableStateOf(this) },
    onChange: (AppData) -> Unit
) = AppDialog(
    title = "Setting",
    validation = { appData.value.run { scanRate().size == 2 } },
    onConfirmed = { onChange(appData.value); closeDialog() },
    onDismissed = { closeDialog() },
    onOpen = { appData.value = this@SettingDialog },
) {
    Settings { appData.value = it }
}

val oidSysDescOid = "1.3.6"
val oidUptime = "1.3.6.1.2.1.1.3"
val oidPrtGeneralSerialNumber = "1.3.6.1.2.1.43.5.1.1.17"
fun devInfoPdu(reqId: Int? = null) = PDU(
    strOids = listOf(oidSysDescOid, oidUptime, oidPrtGeneralSerialNumber),
    reqId = reqId,
)

fun scanPdu(reqId: Int? = null) = PDU(reqId = reqId)

suspend fun snmpGetDevInfo(ip: String) =
    snmpUnicast(ip, retries = 4, interval = 1.seconds, pdu = devInfoPdu(ip.toIpV4UInt().toInt()))

@Composable
fun Mfp.StatusRow(
    globalSnmpThrottle: RateLimiter,
    updateInterval: Duration,
    onRemove: (ip: String) -> Unit
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(4.dp)
) {
    var uptime by remember { mutableStateOf(0.minutes) }
    var model by remember { mutableStateOf("-") }
    var serial by remember { mutableStateOf("-") }
    var errStatus: String? by remember { mutableStateOf(null) }
    var color by remember { mutableStateOf(Black) }
    val update = remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        delay(Random.nextLong(199).milliseconds)
        while (true) {
            runCatching {
                globalSnmpThrottle.runIntermittent {
                    snmpGetDevInfo(ip).onResponse {
                        errStatus = null
                        model = it.received.response[0].variable.toString()
                        serial = it.received.response[2].variable.toString()
                        uptime = it.received.response[1].variable.toLong().milliseconds * 10
                        color = Green
                    }.onTimeout { errStatus = "no respons"; color = Red }
                }
            }.onFailure { errStatus = "${it.message}"; color = Red }
            ++update.value
            if (updateInterval <= 0.seconds) break
            delay(updateInterval)
        }
    }
    OneShotColorAnimationSample(color to color.copy(alpha = 0.5f), update)
    Text(ip, modifier = Modifier.width(120.dp))
    if (errStatus != null) {
        Text(errStatus!!, modifier = Modifier.weight(1f))
    } else {
        Text("${uptime.inWholeSeconds}s", modifier = Modifier.width(120.dp))
        Text(model, modifier = Modifier.weight(1f), maxLines = 1)
        Text(serial, modifier = Modifier.weight(1f), maxLines = 1)
    }
    IconButton(modifier = Modifier.height(24.dp), onClick = { onRemove(ip) }) {
        Icon(Icons.Default.Delete, "delete")
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalTime::class)
@Composable
fun OneShotColorAnimationSample(tColor: Pair<Color, Color>, update: MutableState<Int>) {
    val animatedColor = remember { Animatable(tColor.first) }
    LaunchedEffect(update.value) {
        animatedColor.snapTo(tColor.first)
        animatedColor.animateTo(
            targetValue = tColor.second,
            animationSpec = tween(durationMillis = 1000)
        )
    }
    Box(modifier = Modifier.size(14.dp).background(animatedColor.value))
}

@Composable
fun AppData.MfpAddField(rateLimiter: RateLimiter, onChange: (AppData) -> Unit) {
    fun AppData.addMfps(r: String) = onChange(
        copy(mfps = mfps.toMutableMap().apply {
            r.toIpV4RangeSet().asFlatSequence { it + 1U }.forEach { ip ->
                val adr = ip.toIpV4String()
                this[adr] = Mfp(ip = adr, port = 161, v1CommStr = "public")
            }
        })
    )

    var scanning by remember { mutableStateOf(false) }

    @Composable
    fun RangeField() {
        var ip by remember { mutableStateOf(scanRange) }
        val isError = runCatching { ip.toIpV4RangeSet() }.isFailure
        val ipsStatus: String = runCatching {
            val nIpAdr = ip.toIpV4RangeSet().totalLength()
            val tReq = scanSnmp.intervalMS.milliseconds * (scanSnmp.retries + 1)
            val (n, d) = scanRate()
            val tReqTotal = (tReq + nIpAdr.toInt().seconds / (n.seconds / d.milliseconds))
            if (nIpAdr > 0UL) "$nIpAdr adr, ⌛ideal scan $tReqTotal" else ""
        }.getOrElse { "-" }
        OutlinedTextField(
            ip,
            singleLine = true,
            isError = isError,
            label = { Text(if (ipsStatus == "") "Target Address" else ipsStatus) },
            placeholder = { Text("e.g: 10.0.0.1-10.0.0.254,192.168.1.1") },
            suffix = {},
            leadingIcon = {
                val isMany =
                    runCatching {
                        ip.toIpV4RangeSet().totalLength() + mfps.size.toULong() > 10_000UL
                    }.getOrElse { true }
                IconButton(enabled = !isError && !isMany, onClick = { addMfps(ip) }) {
                    Icon(Icons.Default.Add, "Add")
                }
            },
            trailingIcon = {
                Row {
                    IconButton(enabled = !isError, onClick = SearchDialog(rateLimiter) { onChange(it) }) {
                        Icon(Icons.Default.Search, "Search")
                    }
                    IconButton(enabled = !isError, onClick = { scanning = true }) {
                        Icon(Icons.Default.Search, "Search")
                    }
                }
            },
            onValueChange = {
                ip = it
                runCatching { ip.toIpV4RangeSet() }.onSuccess { onChange(copy(scanRange = ip)) }
            }
        )
    }
    when (scanning) {
        false -> RangeField()
        true -> Scanning(close = { scanning = false }) { onChange(it) }
    }
}

@Composable
fun AppData.Scanning(
    close: () -> Unit,
    onChange: (AppData) -> Unit,
) {
    var scanning by remember { mutableStateOf(true) }
    var cSend = 0
    var cRes = 0
    var status by remember { mutableStateOf("starting...") }
    val snmp = defaultSenderSnmp
    val range = scanRange.toIpV4RangeSet()
    val total = range.totalLength().toLong()
    fun status() = "$cRes(${cRes * 100 / total}%) res / $cSend(${cSend * 100 / total}%) send / $total"

    LaunchedEffect(Unit) {
        range.asUIntFlatSequence().asFlow().scrambled(scanScrambleBlock).throttled(rateLimiter()).map { ip ->
            ++cSend
            status = status()
            Request(
                ip.toIpV4String(),
                commStrV1 = scanSnmp.commStrV1,
                nRetry = scanSnmp.retries,
                interval = scanSnmp.intervalMS.milliseconds,
            )
        }.send(snmp).collect {
            println(it.request.target.address) //todo
            ++cRes
            status = status()
        }
        scanning = false
    }
    @Composable
    fun CloseButton(onClick: () -> Unit) = IconButton(onClick) { Icon(Icons.Default.Close, "close") }

    OutlinedTextField(
        value = status,
        singleLine = true,
        label = { Text(scanRange) },
        leadingIcon = { if (scanning) CircularProgressIndicator() },
        trailingIcon = { CloseButton { close() } },
        onValueChange = {}
    )
}


@OptIn(ExperimentalAtomicApi::class, ExperimentalTime::class)
@Composable
fun AppData.SearchDialog(
    rateLimiter: RateLimiter,
    addMfps: AppData.(Set<String>) -> Map<String, Mfp> = { ips ->
        mfps.toMutableMap().also { ips.map { ip -> it[ip] = Mfp(ip, 161, "public") } }
    },
    ips: SnapshotStateMap<String, String> = remember { mutableStateMapOf() },
    onChange: (AppData) -> Unit,
) = AppDialog(
    "Search and Add MFPs",
    onConfirmed = { onChange(copy(mfps = addMfps(ips.keys))); closeDialog() },
    onDismissed = { closeDialog() },
) {
    val range = scanRange.toIpV4RangeSet()
    val total = range.totalLength().toInt()
    var start by remember { mutableStateOf(now()) }
    var msgCount by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        ips.clear()
        var cSend = 0
        var cRes = 0
        var cDetect = 0
        start = now()
        fun rps(n: Int, dt: Long = (now() - start).inWholeSeconds) = if (dt <= 0) "--rps" else "${n / dt}rps"
        fun count(n: Int) = "$n (${n * 100 / total}%) ${rps(n)}"
        fun dt() = (now() - start).inWholeMilliseconds.milliseconds
        fun msgCount() = "$cDetect found / ${count(cRes)} result / ${count(cSend)} sent / $total total ${dt()}"
        val job = launch {
            while (isActive) {
                msgCount = msgCount()
                delay(0.5.seconds)
            }
        }

        snmpSendFlow(scanRange.toIpV4RangeSet(), rateLimitter = rateLimiter, scrambleBlock = scanScrambleBlock) {
            ++cSend
            Request(
                strAdr = it.toIpV4String(),
                nRetry = scanSnmp.retries,
                interval = scanSnmp.intervalMS.milliseconds,
                commStrV1 = scanSnmp.commStrV1,
                pdu = scanPdu(reqId = it.toInt())
            )
        }.collect { r ->
            ++cRes
            if (r is Result.Response) {
                ++cDetect
                ips[r.request.target.address.inetAddress.toIpV4String()] =
                    r.received.peerAddress.inetAddress.toIpV4String()
            }
        }
        job.cancelAndJoin()
        val t = (now() - start).inWholeSeconds
        msgCount = msgCount()
        running = false
    }

    Text(if (running) "scanning..." else "complete.")
    Text(msgCount)
    if (running) LinearProgressIndicator()

    LazyColumn {
        ips.entries.forEachIndexed { i, (ip, peer) ->
            item {
                Row {
                    Text(i.toString(), Modifier.width(40.dp), maxLines = 1)
                    Text(if (mfps.containsKey(ip)) "☑️" else "➕️", Modifier.width(20.dp))
                    Text(ip, Modifier.width(160.dp), maxLines = 1)
                }
            }
        }
    }
}

class AppDialogScope(val closeDialog: () -> Unit)

@Composable
fun AppDialog(
    title: String? = null,
    text: String = "",
    onConfirmed: (suspend AppDialogScope.() -> Unit)? = null,
    validation: () -> Boolean = { true },
    onDismissed: (suspend AppDialogScope.() -> Unit)? = null,
    onClosed: (suspend () -> Unit)? = null,
    titleWidget: (@Composable () -> Unit)? = title?.let { { Text(it) } },
    scope: CoroutineScope = rememberCoroutineScope(),
    onOpen: (() -> Unit) = {},
    content: @Composable AppDialogScope.() -> Unit = { Text(text) },
): () -> Unit {

    var opened by remember { mutableStateOf(false) }
    if (opened) {
        AlertDialog(
            modifier = Modifier.fillMaxWidth(0.85f),
            onDismissRequest = { scope.launch { onClosed?.let { it() }; opened = false } },
            title = titleWidget,
            text = { Column { AppDialogScope({ opened = false }).content() } },
            confirmButton = onConfirmed?.let { op ->
                {
                    Button(
                        onClick = { scope.launch { AppDialogScope { opened = false }.op() } },
                        enabled = runCatching { validation() }.getOrElse { false }
                    ) { Text("OK") }
                }
            } ?: {},
            dismissButton = onDismissed?.let { op ->
                { Button({ scope.launch { AppDialogScope { opened = false }.op() } }) { Text("Cancel") } }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }
    return { opened = true; onOpen() }
}

@Composable
fun IntField(
    v: Int,
    strLabel: String,
    label: @Composable () -> Unit = { Text(strLabel) },
    sv: MutableState<String> = remember { mutableStateOf(v.toString()) },
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit,
) = TextField(
    sv.value,

    onValueChange = {
        sv.value = it
        runCatching { onValueChange(it.toInt()) }
    },
    label = label,
    modifier = modifier,
    isError = runCatching { sv.value.toInt() }.isFailure,
    singleLine = true,
)


@Serializable
data class AppData(
    val page: Int = 0, // 0=devList,1=Metrics
    val scanRange: String = "",
    val scanScrambleBlock: Int = 10,
    val scanSnmp: SnmpConfig = SnmpConfig(),
    val updateInterval: Int = 10,
    val mfps: Map<String, Mfp> = emptyMap(),
    val scanRate: String = "50/500", // r/ms
    val receiveBufferSize: Int = 1024 * 16,
)

fun AppData.scanRate(): List<Int> =
    scanRate.split("/", limit = 2).map { it.toInt() }.let { if (it.size == 1) it + listOf(1000) else it }


@Serializable
data class SnmpConfig(val intervalMS: Int = 5000, val retries: Int = 5, val commStrV1: String = "public")

@Serializable
data class Mfp(val ip: String, val port: Int, val v1CommStr: String)

val appHome = Path("${System.getProperty("user.home")}/.pau")
val configFile = Path("$appHome/config.json")
var config
    get() = runCatching {
        Json.decodeFromString<AppData>(SystemFileSystem.source(configFile).buffered().readString()).apply {
            require(scanRate()[0] >= 0)
        }
    }.getOrElse { AppData() }
    set(a) = with(SystemFileSystem) {
        if (!exists(configFile)) createDirectories(configFile.parent!!)
        sink(configFile).buffered().use { it.writeString(Json.encodeToString(a)) }
    }
