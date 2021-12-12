// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import jp.`live-on`.shokkaa.mibMapTest
import jp.`live-on`.shokkaa.snmpAgentFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun now() = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    .run { "%04d%02d%02d.%02d%02d%02dZ".format(year, monthNumber, dayOfMonth, hour, minute, second) }

@ExperimentalCoroutinesApi
@Composable
@Preview
fun App() {
    var isRunning by remember { mutableStateOf(true) }
    var logs by remember { mutableStateOf("") }

    MaterialTheme {
        LaunchedEffect(isRunning) {
            while (isRunning) {
                snmpAgentFlow(mibMapTest) { ev, pdu ->
                    val log = "${now().toString()} ${ev.peerAddress}[${ev.pdu[0]}]->[${pdu[0]}]"
                    println(log)
                    logs += log + "\n"
                    pdu
                }.collect { }
                delay(500)
            }
        }
        Column {
            Button(onClick = {
                isRunning = !isRunning
            }) {
                val btnFace = when (isRunning) {
                    true -> "Running/Stop"
                    false -> "Run"
                }
                Text(btnFace)
            }

            Logs(logs)
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

@Composable
fun Logs(logs: String) {
    var logs by remember { mutableStateOf(logs) }
    val stateScroll = rememberScrollState()

    LaunchedEffect(logs) { stateScroll.animateScrollTo(stateScroll.maxValue) }
    Text(logs, modifier = Modifier.verticalScroll(stateScroll))
}
