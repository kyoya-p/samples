import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@Composable
fun Screen() = MaterialTheme {
    Scaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText("Hello, ${getWorld()}!")
            Button(onClick = AppDialog("AAA")) { Text("Button") }
        }
    }
}

@Composable
fun AppDialog(
    text: String = "",
    lazyText: MutableState<String> = remember { mutableStateOf(text) },
    title: @Composable () -> Unit = {},
    onConfirmed: suspend () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = @Composable { Text(lazyText.value) },
): () -> Unit {
    var openDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    if (openDialog) AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = { openDialog = false },
        title = title,
        text = { Column { content() } },
        confirmButton = {
            Button(onClick = { openDialog = false;scope.launch { onConfirmed() } }) {
                Text("OK")
            }
        },
    )
    return { openDialog = true }
}