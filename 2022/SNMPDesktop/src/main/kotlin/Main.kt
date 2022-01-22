import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import jp.wjg.shokkaa.snmp4jutils.SampleOID
import jp.wjg.shokkaa.snmp4jutils.broadcastFlow
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import androidx.ui.material.DataTable


import java.net.InetAddress

var a = 0

@OptIn(ExperimentalMaterialApi::class)
@InternalCoroutinesApi
@Composable
@Preview
fun App() {
    var adrSpec by remember { mutableStateOf("255.255.255.255") }
    var devList by remember { mutableStateOf(mutableSetOf<String>()) }

    MaterialTheme {
        Scaffold {
            Column {
                LaunchedEffect(adrSpec) {
                    println(adrSpec)
                    runCatching {
                        val oids = listOf(SampleOID.hrDeviceDescr.oid, SampleOID.hrDeviceID.oid)
                        devList = mutableSetOf()
                        broadcastFlow(adr = adrSpec, oidList = oids).mapNotNull {
                            "${it.peerAddress.inetAddress.hostAddress} ${it.response[0].variable} ${it.response[1].variable}"
                        }.collect {
                            devList = (devList + it).toMutableSet()
                            delay(300) // effect
                        }
                    }.onFailure { }
                }

                Row {
                    TextField(
                        value = adrSpec,
                        onValueChange = { adrSpec = it }
                    )
                }
                DataTable(
                    columns = 3,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    devList.forEach { adr ->
                        Card(onClick = {}) {
                            Text(adr)
                        }
                        ListItem { }
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
