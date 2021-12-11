// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import jp.`live-on`.shokkaa.mibMapTest
import jp.`live-on`.shokkaa.snmpAgent
import kotlinx.coroutines.delay

@Composable
@Preview
fun App() {
    var isRunning by remember { mutableStateOf(false) }

    MaterialTheme {
        LaunchedEffect(isRunning) {
            while (isRunning) {
                snmpAgent(mibMapTest)
                delay(500)
            }
        }
        Button(onClick = {
            isRunning = !isRunning
        }) {
            Text("$isRunning")
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
