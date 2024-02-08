import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import jp.wjg.shokkaa.snmp4jutils.ULongRangeSet
import jp.wjg.shokkaa.snmp4jutils.async.*
import jp.wjg.shokkaa.snmp4jutils.scanFlow
import kotlinx.coroutines.flow.withIndex

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

fun ULongRangeSet.totalLength() = sumOf { it.endInclusive - it.start + 1UL }
suspend fun scanning(ipSpec: String, progress: (percent: Int) -> Unit, detected: (res: SnmpEvent) -> Unit) {
    fun String.range() = split("-").map { it.toIpV4ULong() }.let { a -> a[0]..a.getOrElse(1) { a[0] } }
    val rangeSet = ULongRangeSet(ipSpec.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { it.range() })
    println(rangeSet.joinToString(",") {
        "${it.start.toIpV4Adr().toIpV4String()}..${it.endInclusive.toIpV4Adr().toIpV4String()}"
    })
    val n = rangeSet.totalLength()
    createDefaultSenderSnmpAsync().use { snmp ->
        snmp.scanFlow(rangeSet) { timeoutEvent = true }.withIndex().collect { (i, r) ->
            progress((i + 1) * 100 / n.toInt())
            if (r.response != null) detected(r)
        }
    }
    println("complete.")

}

@Composable
fun snmpScanDialog(onOk: DialogHandler.() -> Unit) = DialogHandler { dialog ->
    Dialog(title = "SNMP Access Information", onCloseRequest = { dialog.close() }) {
        var ipSpecField by remember { mutableStateOf(app.ipRangeSpec ?: "") }
        var ipSpec by remember { mutableStateOf("") }
        var devs by remember {  mutableStateOf(mutableStateListOf<String>("no-item")) }
        var progress by remember { mutableStateOf("") }
        Column(modifier = Modifier.padding(8.dp).fillMaxSize().size(1024.dp, 768.dp)) {
            TextField(ipSpecField, label = { Text("IP scanning range") }, modifier = Modifier.width(500.dp),
                onValueChange = { ipSpecField = it;app.ipRangeSpec = it;progress = "" })
            Row(verticalAlignment = Alignment.CenterVertically) {
                LaunchedEffect(ipSpec) {
                    scanning(ipSpec, { progress = "$it %" }) { res ->
                        val strRes = "${devs.size}: ${res.peerAddress} ${res.response[0].variable}"
                        devs.addLast(strRes)
                        println(strRes)
                    }
                    progress = "Completed"
                }
                Button(modifier = styleBtn, onClick = { ipSpec = ipSpecField }) { Text("Scan") }
                Button(modifier = styleBtn, onClick = { dialog.close() }) { Text("Close") }
                Text(progress)
            }
            LazyColumn {
                items(devs) { dev -> Card { Text(dev) } }
            }
        }
    }
}


class DialogHandler(val dialogBuilder: @Composable (DialogHandler) -> Unit) {
    private var show by mutableStateOf(false)

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

