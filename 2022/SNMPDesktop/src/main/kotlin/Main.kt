import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import jp.wjg.shokkaa.snmp4jutils.Device
import jp.wjg.shokkaa.snmp4jutils.SampleOID
import jp.wjg.shokkaa.snmp4jutils.broadcastFlow
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import java.net.InetAddress

fun rgb(r: Int, g: Int, b: Int) = Color(r, g, b)
val tealColor = lightColors(
    // https://materialui.co/colors
    primary = rgb(0, 150, 136),
    primaryVariant = rgb(0, 137, 123),
    secondary = rgb(109, 76, 65),
    secondaryVariant = rgb(93, 64, 55),
    background = rgb(236, 239, 241),
    surface = Color.White,
    error = rgb(233, 30, 99),
)


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun App() {
    var adrSpec by remember { mutableStateOf("255.255.255.255") }
    var devList by remember { mutableStateOf(mutableSetOf<Device>()) }

    MaterialTheme(colors = tealColor) {
        Scaffold {
            LaunchedEffect(adrSpec) {
                println(adrSpec)
                runCatching {
                    val oids = listOf(
                        SampleOID.hrDeviceDescr,
                        SampleOID.hrDeviceStatus,
                        //SampleOID.hrDeviceID,
                        //SampleOID.sysDescr,
                        //SampleOID.sysName,
                        //SampleOID.prtGeneralPrinterName,
                        //SampleOID.prtInputVendorName
                    )
                    devList = mutableSetOf()
                    broadcastFlow(adr = adrSpec, oidList = oids.map { it.oid }).mapNotNull {
                        Device(ip = it.peerAddress.inetAddress.hostAddress, vbl = it.response.variableBindings)
                    }.collect {
                        devList = (devList + it).toMutableSet()
                        delay(100) // effect
                    }
                }.onFailure { }
            }

            Column {
                TextField(
                    value = adrSpec,
                    onValueChange = { adrSpec = it }
                )
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    devList.forEach { dev -> card1(dev) }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun card1(dev: Device) = Card(onClick = {}, modifier = Modifier.width(240.dp).padding(2.dp)) {
    @Composable
    fun text(face: String) = Text(face, maxLines = 1, color = MaterialTheme.colors.onPrimary)
    Box(modifier = Modifier.background(MaterialTheme.colors.primaryVariant)) {
        Column {
            text(dev.ip)
            dev.vbl.forEach { text(it.variable.toString()) }
        }
    }
}

@InternalCoroutinesApi
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
//        undecorated = true,
    ) {
        App()
    }
}

fun toHost(adr: ULong) = (3 downTo 0).map { (adr shr it * 8) and 0xffuL }.joinToString(separator = ".") { "$it" }
fun toLong(host: String) =
    InetAddress.getByName(host).address.fold(0uL) { a, e -> (a shl 8) + e.toUByte().toULong() }

fun ipv4Sequence(sHost: String, eHost: String) = ipv4Sequence(toLong(sHost), toLong(eHost))
fun ipv4Sequence(sAdr: ULong, eAdr: ULong) =
    (sAdr..eAdr).asSequence().map { toHost(it) }.map { InetAddress.getByName(it) }
