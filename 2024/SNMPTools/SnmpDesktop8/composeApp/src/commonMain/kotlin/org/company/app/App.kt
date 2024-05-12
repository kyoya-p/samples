import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.serialization.Serializable
import okio.FileSystem
import okio.Path.Companion.toPath

expect val userDir: String
fun appDir() = "$userDir/.snmp-desktop"

inline fun <reified T : @Serializable Any> dataStore(app: String) = storeOf<T>("${appDir()}/$app.json".toPath())

@Serializable
data class AppMain(var page: String = "AGENT")

@Composable
fun App(window: ComposeWindow) = MaterialTheme {
    with(FileSystem.SYSTEM) { createDirectory(appDir().toPath()) }
    var app by remember { mutableStateOf(AppMain()) }
    val store: KStore<AppMain> = dataStore("main")
    when (app.page) {
        "AGENT" -> capturePage(window)
        "SCANNER" -> scannerPage()
    }
}

//@Composable
//internal fun App() = AppTheme {
//    var email by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//    var passwordVisibility by remember { mutableStateOf(false) }
//
//    Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
//
//        Row(
//            horizontalArrangement = Arrangement.Center
//        ) {
//            Text(
//                text = "Login",
//                style = MaterialTheme.typography.titleMedium,
//                modifier = Modifier.padding(16.dp)
//            )
//
//            Spacer(modifier = Modifier.weight(1.0f))
//
//            var isDark by LocalThemeIsDark.current
//            IconButton(
//                onClick = { isDark = !isDark }
//            ) {
//                Icon(
//                    modifier = Modifier.padding(8.dp).size(20.dp),
//                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
//                    contentDescription = null
//                )
//            }
//        }
//
//        OutlinedTextField(
//            value = email,
//            onValueChange = { email = it },
//            label = { Text("Email") },
//            singleLine = true,
//            modifier = Modifier.fillMaxWidth().padding(16.dp)
//        )
//
//        OutlinedTextField(
//            value = password,
//            onValueChange = { password = it },
//            label = { Text("Password") },
//            singleLine = true,
//            visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
//            modifier = Modifier.fillMaxWidth().padding(16.dp),
//            keyboardOptions = KeyboardOptions(
//                keyboardType = KeyboardType.Password
//            ),
//            trailingIcon = {
//                IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
//                    val imageVector = if (passwordVisibility) Icons.Default.Close else Icons.Default.Edit
//                    Icon(imageVector, contentDescription = if (passwordVisibility) "Hide password" else "Show password")
//                }
//            }
//        )
//
//        Button(
//            onClick = { /* Handle login logic here */ },
//            modifier = Modifier.fillMaxWidth().padding(16.dp)
//        ) {
//            Text("Login")
//        }
//
//        TextButton(
//            onClick = { openUrl("https://github.com/terrakok") },
//            modifier = Modifier.fillMaxWidth().padding(16.dp)
//        ) {
//            Text("Open github")
//        }
//    }
//}

internal expect fun openUrl(url: String?)