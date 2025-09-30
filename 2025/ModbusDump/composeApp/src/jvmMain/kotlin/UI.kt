import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock.System.now
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun UI() = MaterialTheme {
    val x = config.copy(result = "")
    var params: AppData by remember { mutableStateOf(x) }
    var run by remember { mutableStateOf(false) }
    Row {
        Column(modifier = Modifier.width(360.dp)) {
            params.ParameterField { params = it }
            Button(onClick = { run = true }) { Text("Dump") }
        }
        SelectionContainer {
            Box(modifier = Modifier.fillMaxSize()) {
                val state = rememberScrollState()
                Text(text = params.result, modifier = Modifier.verticalScroll(state = state))
                VerticalScrollbar(adapter = rememberScrollbarAdapter(scrollState = state))
            }
        }
    }
    LaunchedEffect(run) {
        if (run) with(params) {
            var out = """date: ${now().toLocalDateTime(currentSystemDefault())}
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
                master.read(params).forEach { out += "$it\n" }
                master.disconnect()
            }.onFailure { out += "${it.message}\n${it.stackTraceToString()}" }
            params = copy(result = out)
            println(out)
            run = false
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

