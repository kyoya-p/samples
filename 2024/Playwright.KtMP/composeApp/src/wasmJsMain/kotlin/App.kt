import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    MaterialTheme {
        var user by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        @Composable
        fun uidField() = OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = user,
            onValueChange = { user = it },
            label = { Text("Login ID") },
            singleLine = true
        )

        @Composable
        fun passwordField() = OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = user,
            onValueChange = { user = it },
            label = { Text("Password") },
            singleLine = true
        )
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { loginReq() }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "") }
            },
        ) {
            val stateVertical = rememberScrollState(0)
            VerticalScrollbar(adapter = rememberScrollbarAdapter(stateVertical))
            Column(modifier = Modifier.padding(8.dp).verticalScroll(stateVertical)) {
                uidField()
                passwordField()
            }
        }
    }
}

fun loginReq() {

}