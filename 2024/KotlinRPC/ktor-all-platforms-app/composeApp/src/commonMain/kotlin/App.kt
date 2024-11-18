import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        var greeting by remember { mutableStateOf<String?>(null) }
        val news = remember { mutableStateListOf<String>() }
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
            var showIcon by remember { mutableStateOf(false) }

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
                    it.images.forEach { Text(it.name) }
                } ?: run {
                    Text("Loading information...")
                }

                IconButton(onClick = { showIcon = !showIcon }) {
//                    Text("PULL")
                    Icon(Icons.Default.Add, "Pull Image")
                }

                AnimatedVisibility(showIcon) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(painterResource(Res.drawable.compose_multiplatform), null)
                    }
                }
            }
        }
    }
}
