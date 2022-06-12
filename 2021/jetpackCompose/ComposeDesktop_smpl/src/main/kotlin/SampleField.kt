import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SampleField() {
    var text by remember { mutableStateOf("Initial Value") }
    Column {
        TextField(text, label = { Text("Simple1") }, onValueChange = { text = it })
        TextField(text,
            label = { Text("Modifier.fillMaxWidth") },
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth())
        TextField(text,
            label = { Text(".width(300.dp).padding(8.dp)") },
            onValueChange = { text = it },
            modifier = Modifier.width(300.dp).padding(8.dp))
        PasswordField(text, onValueChange = { text = it })
    }
}

@Composable
fun PasswordField(password: String, onValueChange: (String) -> Unit) = TextField(
    password,
    modifier = Modifier.fillMaxWidth(),
    onValueChange = onValueChange,
    label = { Text("Password") },
    placeholder = { Text("If empty, no change") },
    visualTransformation = PasswordVisualTransformation(),
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
    ),
)

