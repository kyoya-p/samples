import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import jp.wjg.shokkaa.snmp4jutils.broadcastFlow
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import org.snmp4j.Snmp
import org.snmp4j.transport.DefaultUdpTransportMapping


import java.net.InetAddress

@InternalCoroutinesApi
@Composable
@Preview
fun App() {
    var adrSpec by remember { mutableStateOf("255.255.255.255") }

    MaterialTheme {
        Scaffold {
            Column {
                LaunchedEffect(adrSpec) {
                    println("aaaa")
                    val transport = DefaultUdpTransportMapping()
                    val snmp = Snmp(transport)
                    snmp.listen()
                    broadcastFlow(snmp).collect {
                        println(it.inetAddress.hostAddress)
                    }
                    println("bbbb")
                }

                Row {
                    TextField(
                        value = adrSpec,
                        onValueChange = { adrSpec = it }
                    )
                    Button(onClick = {
                        val (s, e) = adrSpec.split("-")
                        ipv4Sequence(s, e).forEach { println(it.hostAddress) }
                    }) {
                        Text("Scan")
                    }
                }
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    repeat(10) {
                        Text("Item $it", modifier = Modifier.padding(2.dp).clickable { println("$it") })
                    }
                }
            }
        }
    }
}

@InternalCoroutinesApi
fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

fun toHost(adr: ULong) = (3 downTo 0).map { (adr shr it * 8) and 0xffuL }.joinToString(separator = ".") { "$it" }
fun toLong(host: String) =
    InetAddress.getByName(host).address.fold(0uL) { a, e -> (a shl 8) + e.toUByte().toULong() }

fun ipv4Sequence(sHost: String, eHost: String) = ipv4Sequence(toLong(sHost), toLong(eHost))
fun ipv4Sequence(sAdr: ULong, eAdr: ULong) =
    (sAdr..eAdr).asSequence().map { toHost(it) }.map { InetAddress.getByName(it) }
