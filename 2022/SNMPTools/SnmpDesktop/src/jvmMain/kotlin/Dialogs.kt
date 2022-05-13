import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Dialog
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Path


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


@Composable
fun FileDialog(
    window: ComposeWindow,
    title: String,
    isLoad: Boolean,
    onResult: (result: Path?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(window, "Choose a file", if (isLoad) LOAD else SAVE) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    if (file != null) {
                        onResult(File(directory).resolve(file).toPath())
                    } else {
                        onResult(null)
                    }
                }
            }
        }.apply {
            this.title = title
        }
    },
    dispose = FileDialog::dispose
)
