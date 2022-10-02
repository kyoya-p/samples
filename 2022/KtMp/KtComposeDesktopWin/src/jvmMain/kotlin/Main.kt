// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.net.ServerSocket

@Composable
@Preview
fun App() = MaterialTheme {
    var socket by remember { mutableStateOf<ServerSocket?>(null) }
    var port by remember { mutableStateOf(0) }

    Scaffold(topBar = { TopAppBar(title = { Text("TCP Port Opener") }) }) {
        Column {
            TextField("$port", onValueChange = { runCatching { port = it.toInt() } })
            Button(onClick = {
                if (socket == null) {
                    socket = ServerSocket(port)
                } else {
                    socket?.close()
                    socket = null
                }
            }) {
                if (socket == null) {
                    Text("Start listening")
                } else {
                    Text("Stop listening")
                }
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
