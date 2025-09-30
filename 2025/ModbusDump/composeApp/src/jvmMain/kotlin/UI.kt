import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
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
import kotlin.time.Clock.System.now
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun UI() = MaterialTheme {
    val x = config.copy(result = "")
    var params: AppData by remember { mutableStateOf(x) }
    var result by remember { mutableStateOf(params.result) }
    var run by remember { mutableStateOf(false) }
    Row {
        Column {
            params.ParameterField { params = it }
            Row {
                Button(enabled = !run, onClick = { run = true }) { Text("Dump") }
                if (run) CircularProgressIndicator()
            }
        }
        Spacer(Modifier.width(4.dp))
        SelectionContainer {
            Box(modifier = Modifier.fillMaxSize()) {
                val scrollState = rememberScrollState()
                Text(text = result, modifier = Modifier.verticalScroll(state = scrollState).fillMaxSize())
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
hostAdr: $hostAdr
unitId: $unitId
mode: ${mode.face}
registerAdr: $regAdr
registerCount: $regCount
bulkSize: $bulkSize

"""
                runCatching {
                    val master = ModbusTCPMaster(hostAdr)
                    master.connect()
                    master.read(params).forEach { result += "$it\n" }
                    master.disconnect()
                }.onFailure { result += "${it.message}\n${it.stackTraceToString()}" }
                val dt = now().toLocalDateTime(currentSystemDefault())
                    .format(LocalDateTime.Format { year(); monthNumber(); day(); hour(); minute(); second() })
                SystemFileSystem.sink(Path(appHome, "moddump-$dt-$hostAdr-$unitId.log")).buffered()
                    .use { it.writeString(result) }
                run = false
            }
        }
    }
    config = params
}

@Composable
fun AppData.ParameterField(onChange: (AppData) -> Unit) = Column {
    TextField(hostAdr, label = { Text("Device address") }, onValueChange = { onChange(copy(hostAdr = it)) })
    IntField(unitId, label = "Unit ID", onValueChange = { onChange(copy(unitId = it)) })
    DropdownMenu(mode, ModbusMode.entries, "Mode", { onChange(copy(mode = it)) }) { _, e -> e.face }

    when (mode) {
        ModbusMode.READ_COILS, ModbusMode.READ_INPUT_DISCRETES -> {
            IntField(regAdr, label = "Data address", onValueChange = { onChange(copy(regAdr = it)) })
            IntField(regCount, label = "# of Bit Length", onValueChange = { onChange(copy(regCount = it)) })
        }

        ModbusMode.READ_HOLDING_REGISTERS, ModbusMode.READ_INPUT_REGISTERS -> {
            IntField(regAdr, label = "Data address", onValueChange = { onChange(copy(regAdr = it)) })
            IntField(regCount, label = "# of Data items", onValueChange = { onChange(copy(regCount = it)) })
            IntField(bulkSize, label = "Data acquisition quantity", onValueChange = { onChange(copy(bulkSize = it)) })
        }
    }
}

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
        runCatching { onValueChange(it.toInt()) }
    },
    label = { Text(label) },
    modifier = modifier,
    isError = runCatching { sv.value.toInt() }.isFailure
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
inline fun <reified E : Enum<E>> DropdownMenu(
    selectedOption: E,
    options: Iterable<E>,
    label: String,
    crossinline onChange: (E) -> Unit,
    modifier: Modifier = Modifier,
    crossinline itemFace: (ix: Int, e: E) -> String,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        TextField(
            value = itemFace(-1, selectedOption),
            onValueChange = { },
            readOnly = true,
            trailingIcon = { TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true),
            label = { Text(label) }
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(itemFace(i, option)) },
                    onClick = { onChange(option); expanded = false },
                )
            }
        }
    }
}
