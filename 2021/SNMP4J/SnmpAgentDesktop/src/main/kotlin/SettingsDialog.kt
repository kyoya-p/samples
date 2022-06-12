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
import jp.wjg.shokkaa.snmp4jutils.walk
import org.snmp4j.CommunityTarget
import org.snmp4j.Snmp
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.net.InetAddress

@Composable
fun SettingsDialog(onClose: () -> Unit, updateIp: (ip: String) -> Unit) = Dialog(onCloseRequest = onClose) {
    val modBtn = Modifier.padding(8.dp)
    var ip by remember { mutableStateOf("192.168.1.2") }

    Column(modifier = Modifier.padding(8.dp)) {
        TextField(ip, onValueChange = { ip = it })
        Row {
            Button(modifier = modBtn, onClick = { updateIp(ip) }) { Text("Scan") }
            Button(modifier = modBtn, onClick = onClose) { Text("Cancel") }
        }
    }
}

val scan = { ip: String ->
    val ipAddr = InetAddress.getByName(ip)!!
    val udpAddr = UdpAddress(ipAddr, 161)
    val target = CommunityTarget(udpAddr, OctetString("public"))
    val snmp = Snmp(DefaultUdpTransportMapping()).apply { listen() }
    walk(ip, listOf("1.3"), snmp).flatMap { it }.toList()
}

