import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

//@Preview
@Composable
fun snmpCaptureDialog(app: WalkerData, onOk: (app: WalkerData) -> Unit) = DialogHandler { dialog ->
    Dialog(title = "SNMP Access Information", onCloseRequest = { dialog.close() }) {
        val styleBtn = Modifier.padding(8.dp)
        var ip by remember { mutableStateOf(app.ip) }
        var comm by remember { mutableStateOf(app.commStr) }
        Column(modifier = Modifier.padding(8.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                OutlinedTextField(ip, label = { Text("IP") }, onValueChange = { ip = it })
                OutlinedTextField(comm, label = { Text("Community String") }, onValueChange = { comm = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                Button(onClick = { onOk(app.copy(ip = ip, commStr = comm)); dialog.close() }) { Text("Capture") }
                Button(onClick = { dialog.close() }) { Text("Cancel") }
            }
        }
    }
}

class DialogHandler(val dialogBuilder: @Composable (DialogHandler) -> Unit) {
    var show by mutableStateOf(false)

    @Composable
    fun placing() {
        if (show) dialogBuilder(this)
    }

    fun open() {
        show = true
    }

    fun close() {
        show = false
    }
}

