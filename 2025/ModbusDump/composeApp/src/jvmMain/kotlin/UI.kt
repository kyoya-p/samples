import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import kotlin.enums.EnumEntries

@Composable
fun UI() = MaterialTheme {
    val x = config
    var params: AppData by remember { mutableStateOf(x) }
    var run by remember { mutableStateOf(false) }
    Row {
        Column {
            params.ParameterField { params = it }
            Button(onClick = { run = true }) { Text("Dump") }
        }
        Column {
            Box(modifier = Modifier.fillMaxSize()) {
                val state = rememberScrollState()
                Text(text = params.result, modifier = Modifier.verticalScroll(state = state))
                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp),
                    adapter = rememberScrollbarAdapter(scrollState = state)
                )
            }
        }
    }
    config = params

    LaunchedEffect(run) {
        if (run) with(config) {
            var out = """hostAdr: $hostAdr
                    |registerAdr: $regAdr
                    |registerCount: $regCount
                    |bulkSize: $bulkSize
                    |
                """.trimMargin()
            runCatching {
                val master = ModbusTCPMaster(hostAdr)
                master.connect()
                master.modbusScan(unitId, regAdr, regCount, bulkSize, mode)
                    .forEach { out += it.toText() + "\n" }
                master.disconnect()
            }.onFailure { out += "${it.message}\n${it.stackTraceToString()}" }
            config = copy(result = result + out)
            run = false
        }
    }
}

@Composable
fun AppData.ParameterField(onChange: (AppData) -> Unit) = Column {
    TextField(hostAdr, label = { Text("Device address") }, onValueChange = { onChange(copy(hostAdr = it)) })
    IntField(unitId, label = "Unit ID", onValueChange = { onChange(copy(unitId = it)) })
    EnumDropdownMenu(selectedOption = mode, options = ModbusMode.entries) { onChange(copy(mode = it)) }

    when (mode) {
        ModbusMode.READ_COILS -> {
            IntField(regAdr, label = "Data address", onValueChange = { onChange(copy(regAdr = it)) })
            IntField(regCount, label = "# of Bit Length", onValueChange = { onChange(copy(regCount = it)) })
        }
        ModbusMode.READ_HOLDING_REGISTERS -> {
            IntField(regAdr, label = "Data address", onValueChange = { onChange(copy(regAdr = it)) })
            IntField(regCount, label = "# of Data items", onValueChange = { onChange(copy(regCount = it)) })
            IntField(bulkSize, label = "Data acquisition quantity", onValueChange = { onChange(copy(bulkSize = it)) })
        }

        else -> {}
    }

//
//    var run by remember { mutableStateOf(false) }
//    Button(onClick = {
//        onChange(copy(result = ""))
//        run = true
//    }) { Text("Dump") }
//
//    LaunchedEffect(run) {
//        if (run) {
//            var out = """hostAdr: $hostAdr
//                    |registerAdr: $regAdr
//                    |registerCount: $regCount
//                    |bulkSize: $bulkSize
//                    |
//                """.trimMargin()
//            runCatching {
//                val master = ModbusTCPMaster(hostAdr)
//                master.connect()
//                master.modbusScan(unitId, regAdr, regCount, bulkSize, mode)
//                    .forEach { out += it.toText() + "\n" }
//                master.disconnect()
//            }.onFailure { out += "${it.message}\n${it.stackTraceToString()}" }
//            onChange(copy(result = result + out))
//            run = false
//        }
//    }
}


@Composable
fun ReadHoldingRegisters(onResult: (String) -> Unit) {
    var offset by remember { mutableStateOf(config.regAdr) }
    var length by remember { mutableStateOf(config.regCount) }
    var windowSize by remember { mutableStateOf(config.bulkSize) }

    var run by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf(config.result) }

    IntField(offset, label = "Data address", onValueChange = { offset = it })
    IntField(length, label = "# of Data items", onValueChange = { length = it })
    IntField(windowSize, label = "Data acquisition quantity", onValueChange = { windowSize = it })

    LaunchedEffect(run) {
        if (run) {
            result += """host: ${config.hostAdr}
                    |offset: $offset
                    |length: $length
                    |windowSize: $windowSize
                    |
                """.trimMargin()
            runCatching {
                val master = ModbusTCPMaster(config.hostAdr)
                master.connect()
                master.modbusScan(config.unitId, offset, length, windowSize, config.mode)
                    .forEach { result += it.toText() + "\n" }
                master.disconnect()
            }.onFailure { result += "${it.message}\n${it.stackTraceToString()}" }
            run = false
        }
    }
}

@Composable
fun IntField(
    v: Int,
    sv: MutableState<String> = remember { mutableStateOf(v.toString()) },
    label: String,
    onValueChange: (Int) -> Unit,
) = TextField(
    sv.value,
    onValueChange = {
        sv.value = it
        runCatching { onValueChange(it.toInt()) }
    },
    label = { Text(label) },
    isError = runCatching { sv.value.toInt() }.isFailure
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
inline fun <reified E : Enum<E>> EnumDropdownMenu(
    selectedOption: E?,
    options: EnumEntries<E>,
    nullable: Boolean = false,
    modifier: Modifier = Modifier,
    crossinline onChange: (E) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        TextField(
            value = selectedOption?.name ?: "",
            onValueChange = { },
            readOnly = true,
            trailingIcon = { TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true),
            label = { Text(options[0].javaClass.simpleName) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
//            if (nullable) androidx.compose.material3.DropdownMenuItem(
//                text = { Text("") },
//                onClick = { onChange(null); expanded = false },
//            )
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = { onChange(option); expanded = false },
                )
            }
        }
    }
}
