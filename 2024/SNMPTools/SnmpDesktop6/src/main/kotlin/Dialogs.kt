import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog


fun snmpCaptureDialog(ipSpec0: String, onOk: (ipSpec: String) -> Unit) = DialogHandler { dialog ->
    Dialog(title = "SNMP Access Information", onCloseRequest = { dialog.close() }) {
        val styleField = Modifier.width(280.dp)
        val styleBtn = Modifier.padding(8.dp)
        var ipSpec by remember { mutableStateOf(ipSpec0) }
        var comm by remember { mutableStateOf(app.commStr ?: "public") }
        Column(modifier = Modifier.padding(8.dp)) {
            Row {
                TextField(ipSpec, label = { Text("IP") }, modifier = styleField,
                    onValueChange = { ipSpec = it;app.ip = it })
                val scan = snmpScanDialog {close()}.apply { placing() }
                Button(modifier = styleBtn, onClick = { scan.open() }) { Text("Scan") }
            }
            TextField(comm, label = { Text("Community String") }, modifier = styleField,
                onValueChange = { comm = it;app.commStr = it })
            Row {
                Button(modifier = styleBtn, onClick = { onOk(ipSpec); dialog.close() }) { Text("Capture") }
                Button(modifier = styleBtn, onClick = { dialog.close() }) { Text("Cancel") }
            }
        }
    }
}

fun snmpScanDialog(onOk: DialogHandler.() -> Unit) = DialogHandler { dialog ->
    Dialog(title = "SNMP Access Information", onCloseRequest = { dialog.close() }) {
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

