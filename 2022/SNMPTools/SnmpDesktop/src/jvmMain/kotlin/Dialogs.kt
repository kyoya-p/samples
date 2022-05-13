import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Dialog
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

data class SNMPV1Info(val ip: String, val commStr: String)

@Composable
fun SnmpAccessInfoDialog(onSettingsClose: (ip: String) -> Unit) = DialogHandler { dialog ->
    Dialog(title = "SNMP Access Information", onCloseRequest = { dialog.close() }) {
        val modBtn = Modifier.padding(8.dp)
        var ip by remember { mutableStateOf("192.168.11.19") }
        var commStr by remember { mutableStateOf("public") }

        Column(modifier = Modifier.padding(8.dp)) {
            TextField(ip, label = { Text("IP") }, onValueChange = { ip = it })
            TextField(commStr, label = { Text("Community String") }, onValueChange = { commStr = it })
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

@Composable
fun AwtFileDialog(
    parent: Frame? = null,
    title: String = "Choose a file",
    mode: Int = FileDialog.LOAD,
    onCloseRequest: (dir: String, file: String) -> Unit,
) = DialogHandler { dialog ->
    AwtWindow(
        create = {
            object : FileDialog(parent, title, mode) {
                override fun setVisible(value: Boolean) {
                    super.setVisible(value)
                    if (value) {
                        if (directory != null && file != null) onCloseRequest(directory, file)
                        dialog.close()
                    }
                }
            }.apply {
                setFilenameFilter { dir, name -> File("$dir/$name").extension == "yaml" }
            }
        },
        dispose = FileDialog::dispose
    )
}

