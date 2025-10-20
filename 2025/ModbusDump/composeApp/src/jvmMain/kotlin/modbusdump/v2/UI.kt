package modbusdump.v2

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import modbusdump.AppData
import modbusdump.appHome
import modbusdump.config
import kotlin.stackTraceToString
import kotlin.time.Clock.System.now
import kotlin.time.ExperimentalTime
import kotlin.use


@OptIn(ExperimentalTime::class)
@Composable
fun UI() = MaterialTheme {
    var params: AppData by remember { mutableStateOf(config) }
    var result by remember { mutableStateOf("") }
    var run by remember { mutableStateOf(false) }
    Row {
        Column {
            params.ParameterField { params = it }
            Row {
                Button(onClick = { run = !run }) { if (!run) Text("Dump") else Text("Stop") }
                if (run) CircularProgressIndicator()
            }
        }
        Spacer(Modifier.width(4.dp))
        SelectionContainer {
            Box(modifier = Modifier.fillMaxSize()) {
                val scrollState = rememberScrollState()
                Text(
                    text = result,
                    modifier = Modifier.verticalScroll(state = scrollState).fillMaxSize(),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace)
                )
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(scrollState = scrollState)
                )
            }
        }
    }
    LaunchedEffect(run) {
        if (run) with(params) {
            launch(Dispatchers.Default) {
                result = """date: ${now().toLocalDateTime(currentSystemDefault())}
host-adr: $hostAdr
unit-id: $unitId
read-mode: ${mode.face}
register-adr: $regAdr
register-count: $regCount
bulk-size: $nAcq

"""
                runCatching {
                    val master = ModbusTCPMaster(hostAdr)
                    master.connect()
//                    master.read(params).forEach { result += "$it\n" }
                    val count = master.read(params) { result += "${it.message}\n" }
                    result += "\n$count\n"
                    master.disconnect()
                }.onFailure { result += "${it.message}\n${it.stackTraceToString()}" }
                val dt = now().toLocalDateTime(currentSystemDefault())
                    .format(LocalDateTime.Format { year(); monthNumber(); day(); hour(); minute(); second() })
                SystemFileSystem.sink(
                    Path(
                        appHome,
                        "moddump-$dt-${hostAdr.filter { it.isDigit() || it == '.' }}-$unitId.log"
                    )
                ).buffered()
                    .use { it.writeString(result) }
                run = false
            }
        }
    }
    config = params
}

@Composable
fun AppData.ParameterField(onChange: (AppData) -> Unit) = Column {
    TcpField { onChange(it) }
    IntField(unitId, label = "Unit ID") { onChange(copy(unitId = it)) }
    DropdownMenu(
        selected = mode,
        options = ReadType.entries.map { it },
        label = "Read Mode",
        itemFace = { _, e -> e.face },
    ) { onChange(copy(mode = it)) }
    IntField(regAdr, label = "Data address") { onChange(copy(regAdr = it)) }
    IntField(regCount, label = "# of Data items") { onChange(copy(regCount = it)) }
    IntField(nAcq, label = "Data acquisition quantity") { onChange(copy(nAcq = it)) }
}


fun String.toIntHex() = trim().run { if (startsWith("0x")) drop(2).toInt(16) else toInt() }
fun String.toTcp(): Pair<String, Int> = (this + ":502").split(":").let { (it[0] to it[1].toIntHex()) }

@Composable
fun AppData.TcpField(
    toTcp: (String) -> AppData = { s -> (s + ":502").split(":").let { copy(it[0], port = it[1].toIntHex()) } },
    toString: () -> String = { "${hostAdr.trim()}${if (port != 502) ":$port" else ""}" },
    sv: MutableState<String> = remember { mutableStateOf(toString()) },
    onValueChange: (AppData) -> Unit,
) = TextField(
    value = sv.value,
    label = { Text("Target (address: $hostAdr, port: $port)") },
    singleLine = true,
    isError = runCatching { toTcp(sv.value) }.isFailure,
    onValueChange = { sv.value = it; runCatching { onValueChange(toTcp(it)) } },
)

@Composable
fun IntField(
    v: Int,
    sv: MutableState<String> = remember { mutableStateOf(v.toString()) },
    label: String,
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit,
) = TextField(
    sv.value,
    onValueChange = {
        sv.value = it
        runCatching { onValueChange(it.toIntHex()) }
    },
    label = {
        val i = runCatching { sv.value.toIntHex().let { "$it / 0x${it.toString(16).uppercase()}" } }.getOrElse { "---" }
        Text("$label ( $i )")
    },
    modifier = modifier,
    isError = runCatching { sv.value.toIntHex() }.isFailure,
    singleLine = true,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
inline fun <reified E> DropdownMenu(
    selected: E,
    options: Iterable<E>,
    label: String,
    crossinline itemFace: (ix: Int, e: E) -> String,
    crossinline onChange: (E) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        TextField(
            value = itemFace(-1, selected),
            onValueChange = { },
            readOnly = true,
            trailingIcon = { TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true),
            label = { Text(label) }
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, option ->
                DropdownMenuItem(
                    text = { Text(itemFace(i, option)) },
                    onClick = { onChange(option); expanded = false },
                )
            }
        }
    }
}
