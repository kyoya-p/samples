import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AppX() {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        val greeting = remember { Greeting().greet() }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource("compose-multiplatform.xml"), null)
                    Text("Compose: $greeting")
                }
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App() = MaterialTheme {
    ScanRange()
}

@Composable
fun ScanRange() {
    var scanSpec by remember { mutableStateOf("192.168.0.1-192.168.255.254") }
    val scanResult by remember { mutableStateOf("No Item") }
    fun scan() {}

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