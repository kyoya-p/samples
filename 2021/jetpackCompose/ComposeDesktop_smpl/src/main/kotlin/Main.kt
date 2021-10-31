import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*

@Composable
fun TitleText(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = typography.h6.copy(fontSize = 14.sp),
        modifier = Modifier.padding(8.dp)
    )
}

@Composable
@Preview
fun AppMain() {
    var dialogId by remember { mutableStateOf(0) }
    Column {
        Button(onClick = { dialogId = 1 }) {
            Text("Open dialog1")
        }
        Button(onClick = { dialogId = 2 }) {
            Text("Open dialog2")
        }
    }
    @Composable
    fun appDialog(w: @Composable DialogWindowScope.() -> Unit) = Dialog(
        onCloseRequest = { dialogId = 0 },
        state = rememberDialogState(position = WindowPosition(Alignment.TopStart)),
        content = w)

    when (dialogId) {
        1 -> appDialog { App1() }
        2 -> appDialog { App1() }
    }
}

enum class NavType {
    HOME, SEARCH, LIBRARY
}

@Composable
@ExperimentalMaterialApi
fun AlertDialog_sample() =
    AlertDialog(onDismissRequest = {},
        title = { Text("Hello title") },
        confirmButton = {
            Button(onClick = {
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(onClick = {
            }) {
                Text("Dismiss")
            }
        },
        text = { Text("Hello text") })


fun main() = application {
    Window(
        title = "Compose Desktop Sample",
        onCloseRequest = ::exitApplication,
    ) {
        App1()
    }
}

