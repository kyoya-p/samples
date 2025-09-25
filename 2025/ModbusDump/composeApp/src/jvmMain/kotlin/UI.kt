import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster

@Composable
fun UI() = MaterialTheme {
    Row {
        var host by remember { mutableStateOf("127.0.0.1") }
        var offset by remember { mutableStateOf(0) }
        var length by remember { mutableStateOf(64) }
        var windowSize by remember { mutableStateOf(1) }
        var result by remember { mutableStateOf("") }
        Column {
            TextField(host, label = { Text("Device address") }, onValueChange = { host = it })
            IntField(offset, label = "Data address", onValueChange = { offset = it })
            IntField(length, label = "# of Data items", onValueChange = { length = it })
            IntField(windowSize, label = "Data acquisition quantity", onValueChange = { windowSize = it })


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
                        master.modbusScan(offset, length, windowSize, ModbusMode.READ_HOLDING_REGISTERS).forEach {
                            result += it.toText() + "\n"
                        }
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