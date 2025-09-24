import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
@Preview
fun UI() = MaterialTheme {
    var showContent by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
//            .background(MaterialTheme.colorScheme.primaryContainer)
//            .safeContentPadding()
//            .fillMaxSize(),
//        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var host by remember { mutableStateOf("127.0.0.1") }
        var adrFrom by remember { mutableStateOf(0) }
        var adrTo by remember { mutableStateOf(1) }
        var windowSize by remember { mutableStateOf(1) }
        var mode by remember { mutableStateOf("") }
        TextField(host, label = { Text("Target Address") }, onValueChange = { host = it })
        IntField(adrFrom, label = "Data Address From", onValueChange = { adrFrom = it })
        IntField(adrTo, label = "Data Address To", onValueChange = { adrTo = it })
        IntField(windowSize, label = "Data Windows Size", onValueChange = { windowSize = it })
        TextField(mode, label = { Text("Mode") }, onValueChange = { mode = it })

        var result by remember { mutableStateOf("no-data") }

        Button(onClick = { showContent = !showContent }) { Text("Dump") }
        SelectionContainer {
            Text(result)
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