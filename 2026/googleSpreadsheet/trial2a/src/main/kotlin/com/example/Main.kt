package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        val link = args[0]
        if (link.contains("code=")) {
            val code = link.substringAfter("code=").substringBefore("&")
            println("Auth code received via deep link: $code")
            GoogleSheetsService.onAuthCodeReceived(code)
            System.exit(0)
        }
    }

    application {
        Window(onCloseRequest = ::exitApplication, title = "SheetMaster Desktop") {
            App()
        }
    }
}

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Ready") }
    var isAuthenticated by remember { mutableStateOf(false) }
    var spreadsheetTitle by remember { mutableStateOf("New Spreadsheet") }
    var spreadsheetId by remember { mutableStateOf("") }
    var range by remember { mutableStateOf("A1") }
    var cellValue by remember { mutableStateOf("") }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("SheetMaster Desktop", style = MaterialTheme.typography.h4)
            Text("Status: $status")

            if (!isAuthenticated) {
                Button(onClick = {
                    scope.launch {
                        status = "Authenticating..."
                        try {
                            withContext(Dispatchers.IO) {
                                GoogleSheetsService.getService()
                            }
                            isAuthenticated = true
                            status = "Authenticated ✅"
                        } catch (e: Exception) {
                            status = "Error: ${e.message}"
                            println("Authentication failed: ${e.message}")
                        }
                    }
                }) {
                    Text("Login with Google")
                }
            } else {
                Spacer(Modifier.height(20.dp))
                
                Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(8.dp)) {
                        Text("Create Spreadsheet", style = MaterialTheme.typography.h6)
                        TextField(spreadsheetTitle, { spreadsheetTitle = it }, label = { Text("Title") })
                        Button(onClick = {
                            scope.launch {
                                status = "Creating..."
                                val id = withContext(Dispatchers.IO) {
                                    GoogleSheetsService.createSpreadsheet(spreadsheetTitle)
                                }
                                spreadsheetId = id
                                status = "Created! ID: $id"
                                withContext(Dispatchers.IO) {
                                    GoogleSheetsService.openInBrowser(id)
                                }
                            }
                        }) {
                            Text("Create")
                        }
                    }
                }

                Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(8.dp)) {
                        Text("Edit Spreadsheet", style = MaterialTheme.typography.h6)
                        TextField(spreadsheetId, { spreadsheetId = it }, label = { Text("ID") })
                        TextField(range, { range = it }, label = { Text("Range (A1)") })
                        TextField(cellValue, { cellValue = it }, label = { Text("Value") })
                        Button(onClick = {
                            scope.launch {
                                status = "Updating..."
                                withContext(Dispatchers.IO) {
                                    GoogleSheetsService.updateCell(spreadsheetId, range, cellValue)
                                }
                                status = "Updated!"
                            }
                        }) {
                            Text("Update Cell")
                        }
                    }
                }
            }
        }
    }
}
