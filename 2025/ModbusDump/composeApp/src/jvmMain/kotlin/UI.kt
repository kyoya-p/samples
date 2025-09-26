import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
    Row {
        var host by remember { mutableStateOf(config.hostAdr) }
        var unitId by remember { mutableStateOf(config.unitId) }
        var offset by remember { mutableStateOf(config.regAdr) }
        var length by remember { mutableStateOf(config.regCount) }
        var windowSize by remember { mutableStateOf(config.bulkSize) }
        var mode by remember { mutableStateOf(config.mode) }
        var result by remember { mutableStateOf(config.result) }
        Column {
            TextField(host, label = { Text("Device address") }, onValueChange = { host = it })
            IntField(unitId, label = "Unit ID", onValueChange = { unitId = it })
            IntField(offset, label = "Data address", onValueChange = { offset = it })
            IntField(length, label = "# of Data items", onValueChange = { length = it })
            IntField(windowSize, label = "Data acquisition quantity", onValueChange = { windowSize = it })
            EnumDropdownMenu(selectedOption = mode, options = ModbusMode.entries) { if (it != null) mode = it }

            var run by remember { mutableStateOf(false) }
            Button(onClick = {
                result = ""
                run = true
            }) { Text("Dump") }

            LaunchedEffect(run) {
                if (run) {
                    result += """host: $host
                    |offset: $offset
                    |length: $length
                    |windowSize: $windowSize
                    |
                """.trimMargin()
                    runCatching {
                        val master = ModbusTCPMaster(host)
                        master.connect()
                        master.modbusScan(unitId, offset, length, windowSize, mode)
                            .forEach { result += it.toText() + "\n" }
                        master.disconnect()
                    }.onFailure { result += "${it.message}\n${it.stackTraceToString()}" }
                    run = false
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            val state = rememberScrollState()
            Text(text = result, modifier = Modifier.verticalScroll(state = state))
            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp),
                adapter = rememberScrollbarAdapter(scrollState = state)
            )
        }
        config = config.copy(
            hostAdr = host,
            unitId = unitId,
            regAdr = offset,
            regCount = length,
            bulkSize = windowSize,
            mode = mode,
            result = result,
        )
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
    crossinline onChange: (E?) -> Unit,
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
            if (nullable) androidx.compose.material3.DropdownMenuItem(
                text = { Text("") },
                onClick = { onChange(null); expanded = false },
            )

            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = { onChange(option); expanded = false },
                )
            }
        }
    }
}
