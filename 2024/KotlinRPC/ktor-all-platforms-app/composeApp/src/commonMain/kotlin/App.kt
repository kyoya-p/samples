import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.ktor.client.*
import io.ktor.http.*
import kotlinx.rpc.krpc.ktor.client.installRPC
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.krpc.streamScoped
import kotlinx.rpc.withService
import ktor_all_platforms_app.composeapp.generated.resources.Res
import ktor_all_platforms_app.composeapp.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource

expect val DEV_SERVER_HOST: String

val client by lazy { HttpClient { installRPC() } }

@Composable
fun App() {
    var serviceOrNull: UserService? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        serviceOrNull = client.rpc {
            url {
                host = DEV_SERVER_HOST
                port = 8080
                encodedPath = "/api"
            }
            rpcConfig { serialization { json() } }
        }.withService()
    }

    val service = serviceOrNull // for smart casting
    if (service != null) {
//        var greeting by remember { mutableStateOf<String?>(null) }
//        val news = remember { mutableStateListOf<String>() }
        var status by remember { mutableStateOf<CtStatus?>(null) }
//        LaunchedEffect(service) {
//            greeting = service.hello(
//                "User from ${getPlatform().name} platform",
//                UserData("Berlin", "Smith")
//            )
//        }
//
//        LaunchedEffect(service) {
//            streamScoped {
//                service.subscribeToNews().collect { article ->
//                    news.add(article)
//                }
//            }
//        }
        LaunchedEffect(service) {
            streamScoped {
                service.status().collect { status = it }
            }
        }

        MaterialTheme {
            var showDialog by remember { mutableStateOf(false) }

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
//                greeting?.let {
//                    Text(it)
//                } ?: run {
//                    Text("Establishing server connection...")
//                }

//                news.forEach {
//                    Text("Article: $it")
//                }

                status?.let {
                    if (it.images.isEmpty()) Text("No Images.")
                    it.images.forEach { imageItem(it) }
                } ?: run {
                    Text("Loading information...")
                }
                IconButton(onClick = { showDialog = true }) { Icon(Icons.Default.Add, "Pull Image") }
                var pullReq by remember { mutableStateOf<(suspend () -> Unit)?>(null) }
                LaunchedEffect(pullReq) {
                    pullReq?.invoke()
                    pullReq = null
                }
                if (showDialog) pullImageDialog(onClose = { showDialog = false }) {
                    pullReq = { status = service.pullImage(it) }
                    showDialog = false
                }

                console(service)
            }


//                AnimatedVisibility(showIcon) {
//                    Column(
//                        Modifier.fillMaxWidth(),
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        Image(painterResource(Res.drawable.compose_multiplatform), null)
//                    }
//                }
        }
    }
}


@Composable
fun imageItem(img: Image) = Card {
    Row {
        Text(img.name)
        IconButton(onClick = {}) { Icon(Icons.Default.Delete, "delete") }
    }
}

@Composable
fun pullImageDialog(onClose: () -> Unit, onOk: (String) -> Unit) = Dialog(onDismissRequest = onClose) {
    Card {
        var id by remember { mutableStateOf("") }
        Column(modifier = Modifier.padding(8.dp)) {
            TextField(value = id, label = { Text("IMAGE ID:") }, onValueChange = { id = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onOk(id) }) { Text("PULL") }
                Button(onClick = onClose) { Text("CANCEL") }
            }
        }
    }
}

@Composable
fun console(service: UserService) {
    var req by remember { mutableStateOf<(suspend () -> Unit)?>(null) }
    LaunchedEffect(req) {
        req?.invoke()
        req = null
    }
    var cmd by remember { mutableStateOf("") }
    var logs by remember { mutableStateOf("") }
    Column {
        Text(logs, maxLines = 7, minLines = 7)
        TextField(cmd, onValueChange = { cmd = it }, modifier = Modifier.onKeyEvent { ev ->
            if (ev.key == Key.Enter) {
                val cs = cmd.trim().split(" ")
                logs += "+$cs"
                req = { service.process(cs) { logs += it.decodeToString() } }
                cmd = ""
            }
            true
        })
    }
}
