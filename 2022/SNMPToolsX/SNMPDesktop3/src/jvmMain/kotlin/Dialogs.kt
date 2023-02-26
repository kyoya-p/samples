import androidx.compose.desktop.ui.tooling.preview.Preview
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
import jp.wjg.shokkaa.snmp4jutils.async.*
import jp.wjg.shokkaa.snmp4jutils.ipV4AddressRangeSequence
import jp.wjg.shokkaa.snmp4jutils.toIPv4ULong
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import java.awt.FileDialog
import java.awt.Frame
import java.net.InetAddress

fun snmpTarget(ip: String, port: Int, comm: String) =
    SnmpTarget(UdpAddress(InetAddress.getByName(ip), port), OctetString(comm))

val SnmpTarget.ip get() = address.inetAddress.hostName!!
val SnmpTarget.port get() = address.port
val SnmpTarget.comm get() = community.value!!.decodeToString()

//@OptIn(ExperimentalTime::class)
@Preview
@Composable
fun SnmpCaptureDialog(ipSpec0: String, onOk: (ipSpec: String) -> Unit) = DialogHandler { dialog ->
    Dialog(title = "SNMP Access Information", onCloseRequest = { dialog.close() }) {
        val styleBtn = Modifier.padding(8.dp)
        var ipSpec by remember { mutableStateOf(ipSpec0) }
        var comm by remember { mutableStateOf(app.commStr ?: "public") }
        Column(modifier = Modifier.padding(8.dp)) {
            TextField(ipSpec, label = { Text("IP") }, onValueChange = { ipSpec = it;app.ip = it })
            TextField(comm, label = { Text("Community String") }, onValueChange = { comm = it;app.commStr = it })
            Row {
                Button(modifier = styleBtn, onClick = { onOk(ipSpec); dialog.close() }) { Text("OK") }
                Button(modifier = styleBtn, onClick = { dialog.close() }) { Text("Cancel") }
            }
        }
    }
}

fun ipSequence(ipSpec: String) = ipSpec.split(",").asSequence().flatMap { ipRange ->
    val range = ipRange.split("-")
    when {
        range.size == 1 -> sequenceOf(range.first()).map { InetAddress.getByName(it)!! }
        range.size == 2 -> {
            val (s, e) = range.map { if (it == "") error("Illegal IP") else InetAddress.getByName(it)!! }
            println("s=${range[0]}/${s.hostAddress}/${s.toIPv4ULong()} e=${range[1]}/${e.toIPv4ULong()}")
            ipV4AddressRangeSequence(s, e)
        }

        else -> sequenceOf()
    }
}

@Composable
fun SnmpScanDialog(onClose: (SnmpTarget) -> Unit) = DialogHandler { dialog ->
    Dialog(title = "SNMP Scan and Capture", onCloseRequest = { dialog.close() }) {
        val styleBtn = Modifier.padding(8.dp)
        var ipRange by remember { mutableStateOf(app.ipRange) }
        var comm by remember { mutableStateOf(app.commStr) }
        val ipList = mutableListOf<InetAddress>()

        Column(modifier = Modifier.padding(8.dp)) {
            TextField(ipRange, label = { Text("Start-End") }, onValueChange = { ipRange = it;app.ipRange = it; })
            TextField(comm, label = { Text("Community String") }, onValueChange = { comm = it;app.commStr = it })
            Row {
                Button(modifier = styleBtn, onClick = {
                    dialog.close()
                    onClose(snmpTarget(ipRange, 161, comm))
                }) { Text("OK") }
                Button(modifier = styleBtn, onClick = { dialog.close() }) { Text("Cancel") }
            }
            AutoScrollBox {
                Column {
                    ipList.forEach { Button(onClick = {}) { Text("[${it.hostName}]") } }
                }
            }
        }
        LaunchedEffect(ipRange) {
            val (start, end) = ipRange.split("-").mapNotNull { InetAddress.getByName(it) }
            val snmp = SnmpBuilder().udp().v1().v3().build().async().listen()
            ipV4AddressRangeSequence(start, end).forEach { adr ->
                snmp.send(
                    PDU(PDU.GETNEXT, listOf(VariableBinding(org.snmp4j.smi.OID(SampleOID.sysDescr.oid)))),
                    CommunityTarget(UdpAddress(adr, 161), OctetString("public"))
                )
                //res?.peerAddress
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
            }
        },
        dispose = FileDialog::dispose
    )
}

