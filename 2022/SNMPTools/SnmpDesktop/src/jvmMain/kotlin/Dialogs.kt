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
import org.snmp4j.CommunityTarget
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.InetAddress

typealias SnmpTarget = CommunityTarget<UdpAddress>

fun snmpTarget(ip: String, port: Int, comm: String) =
    SnmpTarget(UdpAddress(InetAddress.getByName(ip), port), OctetString(comm))

val SnmpTarget.ip get() = address.inetAddress.hostName!!
val SnmpTarget.port get() = address.port
val SnmpTarget.comm get() = community.value!!.decodeToString()
fun SnmpTarget.copy(ip: String = this.ip, port: Int = this.port, comm: String = this.comm) = snmpTarget(ip, port, comm)


val defaultCommunityTarget = snmpTarget("localhost", 161, "public")

@Composable
fun SnmpAccessInfoDialog(onSettingsClose: (v1: SnmpTarget) -> Unit) = DialogHandler { dialog ->
    Dialog(title = "SNMP Access Information", onCloseRequest = { dialog.close() }) {
        val styleBtn = Modifier.padding(8.dp)
        var v1 by remember { mutableStateOf(defaultCommunityTarget) }

        Column(modifier = Modifier.padding(8.dp)) {
            TextField(v1.ip, label = { Text("IP") }, onValueChange = { v1 = v1.copy(ip = it) })
            TextField(v1.comm, label = { Text("Community String") }, onValueChange = { v1 = v1.copy(comm = it) })
            Row {
                Button(modifier = styleBtn, onClick = { dialog.close(); onSettingsClose(v1) }) { Text("OK") }
                Button(modifier = styleBtn, onClick = { dialog.close() }) { Text("Cancel") }
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

