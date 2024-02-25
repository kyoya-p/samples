import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var scanSpec by remember { mutableStateOf("192.168.0.1-192.168.255.254") }
    val scanResult by remember { mutableStateOf("No Item") }
    fun scan() {}

//    val config = RealmConfiguration.create(schema = setOf(Item::class))
//    val realm: Realm = Realm.open(config)
//    LaunchedEffect(Unit) {
//        val app = realm.query<Item>().find()[0]
//        println(app)
//    }

    Scaffold(
        modifier = Modifier.padding(8.dp),
//        topBar = { TopAppBar { Text("Scan IP Range") } },
        floatingActionButton = {
            FloatingActionButton(onClick = ::scan) { Icon(Icons.Default.Search, "IP Range Scan") }
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
                scanResult,
                readOnly = true,
                onValueChange = { },
                label = { Text("Result") },
                singleLine = false
            )
        }
    }
}