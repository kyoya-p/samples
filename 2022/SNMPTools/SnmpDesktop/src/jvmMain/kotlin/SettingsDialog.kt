import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun SnmpAccessInfoDialog(onSettingsClose: (ip: String) -> Unit) = DialogHandler { dialog ->
    Dialog(title = "SNMP Access Information", onCloseRequest = { dialog.close() }) {
        val modBtn = Modifier.padding(8.dp)
        var ip by remember { mutableStateOf("192.168.11.19") }

        Column(modifier = Modifier.padding(8.dp)) {
            TextField(ip, label = { Text("IP") }, onValueChange = { ip = it })
            Row {
                Button(modifier = modBtn, onClick = {
                    dialog.close()
                    onSettingsClose(ip)
                }) { Text("OK") }
                Button(modifier = modBtn, onClick = { dialog.close() }) { Text("Cancel") }
            }
        }
    }
}



