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
fun SettingsDialog(onClose: () -> Unit) = Dialog(onCloseRequest = onClose) {
    val modBtn = Modifier.padding(8.dp)
    var ip by remember { mutableStateOf("192.168.1.2") }

    Column(modifier = Modifier.padding(8.dp)) {
        TextField(ip, onValueChange = { ip = it })
        Row {
            Button(modifier = modBtn, onClick = { }) { Text("OK") }
            Button(modifier = modBtn, onClick = onClose) { Text("Cancel") }
        }
    }
}

