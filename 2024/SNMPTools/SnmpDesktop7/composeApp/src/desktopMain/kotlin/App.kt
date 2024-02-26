import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.wjg.shokkaa.snmp4jutils.async.createDefaultSenderSnmpAsync
import jp.wjg.shokkaa.snmp4jutils.filterResponse
import jp.wjg.shokkaa.snmp4jutils.scanFlow
import jp.wjg.shokkaa.snmp4jutils.toRangeSet
import org.jetbrains.compose.resources.ExperimentalResourceApi

//@OptIn(ExperimentalResourceApi::class)
//@Composable
//fun AppX() {
//    MaterialTheme {
//        var showContent by remember { mutableStateOf(false) }
//        val greeting = remember { Greeting().greet() }
//        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
//            Button(onClick = { showContent = !showContent }) {
//                Text("Click me!")
//            }
//            AnimatedVisibility(showContent) {
//                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
//                    Image(painterResource("compose-multiplatform.xml"), null)
//                    Text("Compose: $greeting")
//                }
//            }
//        }
//    }
//}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App() = MaterialTheme {
    ScanRange()
}

//data class App(val uid: String, val range: String)

//class Item() : RealmObject {
//    @PrimaryKey
//    var _id: ObjectId = ObjectId()
//    var summary: String = ""
//    var owner_id: String = ""
//    constructor(ownerId: String = "") : this() {
//        owner_id = ownerId
//    }
//}

@Composable
fun ScanRange() {
    var scanSpec by remember { mutableStateOf("10.36.102.1-10.36.102.254") }
    var scanResult by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    LaunchedEffect(scanning) {
        if (scanning) {
            val rangeSet = scanSpec.toRangeSet()
            scanResult = ""
            createDefaultSenderSnmpAsync().run {
                scanFlow(rangeSet) {
                    target?.timeout = 3000
                    target?.retries = 2
                }.filterResponse().collect {
                    scanResult += "${it.received.peerAddress},\n"
                }
            }
            scanning = false
        }
    }

    Scaffold(
        modifier = Modifier.padding(8.dp),
//        topBar = { TopAppBar { Text("Scan IP Range") } },
        floatingActionButton = {
            FloatingActionButton(onClick = { scanning = true }) {
                when (scanning) {
                    true -> CircularProgressIndicator()
                    false -> Icon(Icons.Default.Search, "IP Range Scan")
                }
            }
        }
    ) {
        Column {
            OutlinedTextField(
                scanSpec,
                onValueChange = { scanSpec = it },
                label = { Text("IP Range") },
                singleLine = false
            )
            OutlinedTextField(
                scanResult.ifEmpty { "No Item" },
                readOnly = true,
                onValueChange = { },
                label = { Text("Result") },
                singleLine = false
            )
        }
    }
}

