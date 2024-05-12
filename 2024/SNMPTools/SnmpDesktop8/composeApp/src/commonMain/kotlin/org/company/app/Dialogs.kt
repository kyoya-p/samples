//package org.company.app

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
//import androidx.compose.material.Button
//import androidx.compose.material.Text
//import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog


@Preview
@Composable
fun snmpCaptureDialog(app:WalkerData, ipSpec0: String, onOk: (ipSpec: String) -> Unit) = DialogHandler { dialog ->
    Dialog(title = "SNMP Access Information", onCloseRequest = { dialog.close() }) {
        val styleBtn = Modifier.padding(8.dp)
        var ipSpec by remember { mutableStateOf(ipSpec0) }
        var comm by remember { mutableStateOf(app.commStr ?: "public") }
        Column(modifier = Modifier.padding(8.dp)) {
            TextField(ipSpec, label = { Text("IP") }, onValueChange = { ipSpec = it;app.ip = it })
            TextField(comm, label = { Text("Community String") }, onValueChange = { comm = it;app.commStr = it })
            Row {
                Button(modifier = styleBtn, onClick = { onOk(ipSpec); dialog.close() }) { Text("Capture") }
                Button(modifier = styleBtn, onClick = { dialog.close() }) { Text("Cancel") }
            }
        }
    }
}

class DialogHandler(val dialogBuilder: @Composable (DialogHandler) -> Unit) {
    var show by mutableStateOf(false)

    @Composable
    fun placing() {
        if (show) dialogBuilder(this)
    }

    fun open() {
        show = true
    }

    fun close() {
        show = false
    }
}

