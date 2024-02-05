import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import jp.wjg.shokkaa.snmp4jutils.ULongRangeSet
import jp.wjg.shokkaa.snmp4jutils.async.toIpV4Adr
import jp.wjg.shokkaa.snmp4jutils.async.toIpV4ULong

// Common Component Styles
val styleBtn = Modifier.padding(8.dp)

@Composable
fun snmpCaptureDialog(ipSpec0: String, onOk: (ipSpec: String) -> Unit) = DialogHandler { dialog ->
    Dialog(title = "SNMP Access Information", onCloseRequest = { dialog.close() }) {
        var ipSpec by remember { mutableStateOf(ipSpec0) }
        var comm by remember { mutableStateOf(app.commStr ?: "public") }
        Column(modifier = Modifier.padding(8.dp)) {
            Row {
                TextField(ipSpec, label = { Text("IP") }, modifier = Modifier.width(280.dp),
                    onValueChange = { ipSpec = it;app.ip = it })
                val scan = snmpScanDialog { close() }.apply { placing() }
                Button(modifier = styleBtn, onClick = { scan.open() }) { Text("Scan") }
            }
            TextField(comm, label = { Text("Community String") }, modifier = Modifier.width(280.dp),
                onValueChange = { comm = it;app.commStr = it })
            Row {
                Button(modifier = styleBtn, onClick = { onOk(ipSpec); dialog.close() }) { Text("Capture") }
                Button(modifier = styleBtn, onClick = { dialog.close() }) { Text("Cancel") }
            }

        }
    }
}

val fScan = {}

@Composable
fun snmpScanDialog(onOk: DialogHandler.() -> Unit) = DialogHandler { dialog ->
    Dialog(title = "SNMP Access Information", onCloseRequest = { dialog.close() }) {
        var ipSpecField by remember { mutableStateOf("192.168.0.1-192.168.0.254, 192.168.127.1-192.168.127.254") }
        var ipSpec by remember { mutableStateOf("") }
        val devs by remember { mutableStateOf(listOf<String>("empty")) }
        Column(modifier = Modifier.padding(8.dp)) {
            TextField(ipSpecField, label = { Text("IP scanning range") }, modifier = Modifier.width(500.dp),
                onValueChange = { ipSpecField = it })

            Row {
                LaunchedEffect(ipSpec) {
                    val range = ipSpec.split(",").map { it.split("-").map { it.toIpV4Adr().toIpV4ULong() } }
                        .map { it[0]..if (it.size < 2) it[0] else it[1] }.fold(ULongRangeSet()) { a, e -> a.add(e) }


                    println(ipSpec)
                }
                Button(modifier = styleBtn, onClick = { ipSpec = ipSpecField }) { Text("Scan") }
                Button(modifier = styleBtn, onClick = { dialog.close() }) { Text("Close") }
            }
            LazyColumn {
                items(devs) { dev ->
                    Card { Text(dev) }
                }
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

