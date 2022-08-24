// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import jp.wjg.shokkaa.snmp4jutils.mibMapTest
import jp.wjg.shokkaa.snmp4jutils.snmpAgent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Suppress("unused")
fun now() = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    .run { "%04d%02d%02d.%02d%02d%02dZ".format(year, monthNumber, dayOfMonth, hour, minute, second) }

//val agentList = mutableListOf<SnmpAgent>()

@ExperimentalCoroutinesApi
@Composable
@Preview
fun App(window: ComposeWindow) = MaterialTheme {
    var isRunning by remember { mutableStateOf(true) }
    var isActiveCloningDialog by remember { mutableStateOf(false) }
    var isActiveOpenDialog by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf("Logs------------------\n") }

    Scaffold(topBar = {
        TopAppBar {
            //Button(onClick = { isActiveSettingDialog = true }) { Text("Settings") }
            Button(onClick = { isActiveCloningDialog = true }) { Text("Clone") }
        }
    }) {
        LaunchedEffect(isRunning) {
            while (isRunning) {
                snmpAgent(vbl = mibMapTest.values.toList()) {
                    logs += "${it.variableBindings}\n"
                    it
                }
                delay(500)
            }
        }
        Column {
//            Button(onClick = { isRunning = !isRunning }) {
//                val btnFace = when (isRunning) {
//                    true -> "Running/Stop"
//                    false -> "Run"
//                }
//                Text(btnFace)
//            }

            Logs(logs)
        }


        if (isActiveOpenDialog) {
            FileDialog(window = window, title = "Notepad", isLoad = true) {
                isActiveOpenDialog = false
                println(it)
            }

        }

        if (isActiveCloningDialog) SettingsDialog(onClose = { isActiveCloningDialog = false }) {

        }
    }
}

@ExperimentalCoroutinesApi
fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App(window)
    }
}

@Composable
fun Logs(logs: String) {
    val stateScroll = rememberScrollState()

    LaunchedEffect(stateScroll) { stateScroll.animateScrollTo(stateScroll.maxValue) }
    Text(logs, modifier = Modifier.verticalScroll(stateScroll))
}
