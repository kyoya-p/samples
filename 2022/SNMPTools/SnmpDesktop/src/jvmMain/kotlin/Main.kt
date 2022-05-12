// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun currentTime() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
fun today() = currentTime().run { "%04d%02d%02d".format(year, monthNumber, dayOfMonth) }
fun now() = currentTime().run { "%02d%02d%02d".format(hour, minute, second) }

//val agentList = mutableListOf<SnmpAgent>()

@ExperimentalCoroutinesApi
@Composable
@Preview
fun App(window: ComposeWindow) = MaterialTheme {
    var isRunning by remember { mutableStateOf(true) }
    var isActiveCloningDialog by remember { mutableStateOf(false) }
    var isActiveOpenDialog by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf("\n") }

    Scaffold(topBar = {
        TopAppBar {
            //Button(onClick = { isActiveSettingDialog = true }) { Text("Settings") }
            Button(onClick = { isActiveCloningDialog = true }) { Text("Clone") }
        }
    }) {
        LaunchedEffect(isRunning) {
            while (isRunning) {
                logs += "${today()} Start ------------------\n"
                snmpAgent(vbl = mibMapTest.values.toList()) { ev, pdu ->
                    logs += "${now()} ${ev.peerAddress} ${pdu.variableBindings}\n"
                    pdu
                }
                delay(500)
            }
        }
        AutoScrollBox {
            Text(logs, fontSize = 14.sp, modifier = Modifier.fillMaxSize())
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
private fun AutoScrollBox(contents: @Composable() BoxScope.() -> Unit) {
    val stateVertical = rememberScrollState()
    LaunchedEffect(stateVertical.maxValue) { stateVertical.animateScrollTo(stateVertical.maxValue) }
    Box(
        modifier = Modifier.fillMaxSize().verticalScroll(stateVertical)
    ) {
        contents()
    }
}


@Composable
fun LogBox1(logs: String) {
    val stateVertical = rememberScrollState()
    val stateHorizontal = rememberScrollState(0)

    LaunchedEffect(stateVertical) { stateVertical.animateScrollTo(stateVertical.maxValue) }
    Text(
        logs,
        modifier = Modifier.verticalScroll(stateVertical)
            .horizontalScroll(stateHorizontal)

    )
}

@Composable
fun LogBox2(logs: String) = Box(
    modifier = Modifier.fillMaxSize()
) {
    val stateVertical = rememberScrollState()
    val stateHorizontal = rememberScrollState(0)
    LaunchedEffect(stateVertical) { stateVertical.animateScrollTo(stateVertical.maxValue) }
    Text(
        logs, fontSize = 16.sp,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(stateVertical)
            .padding(end = 12.dp, bottom = 12.dp)
            .horizontalScroll(stateHorizontal)
    )
    VerticalScrollbar(
        modifier = Modifier.align(Alignment.CenterEnd)
            .fillMaxHeight(),
        adapter = rememberScrollbarAdapter(stateVertical)
    )
    HorizontalScrollbar(
        modifier = Modifier.align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(end = 12.dp),
        adapter = rememberScrollbarAdapter(stateHorizontal)
    )
}