import com.jakewharton.mosaic.runMosaic
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.layout.padding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import androidx.compose.runtime.*

// Input interface
interface InputProvider {
    suspend fun nextCommand(): String?
}

fun getCurrentTime(): String {
    return java.time.LocalTime.now().toString().take(8)
}

// Layout Constants for Hit Testing
object LayoutConfig {
    const val HEADER_LINES = 8 // Title(1)+Sep(1)+Cmd(1)+Stat(1)+Last(1)+Sep(1)+Head(1)+Sep(1)
    const val COL_REMOVE_START = 80 // Approximate column for [Remove]
}

fun mainApp(inputProvider: InputProvider) = runBlocking {
    val apiKey = System.getenv("API_KEY") ?: "AIzaSyDpE5hkTVWMt8iYPPm30yNL6KJ-YivAwJ4"
    val repo = FirestoreRepository(apiKey, "riot26-70125")

    runMosaic {
        var contacts by remember { mutableStateOf(emptyList<Contact>()) }
        var status by remember { mutableStateOf("Ready") }
        var lastCommand by remember { mutableStateOf("") }
        var debugMsg by remember { mutableStateOf("") } // Debug info
        var loading by remember { mutableStateOf(false) }

        // Initial Load & Realtime Listen
        LaunchedEffect(Unit) {
            loading = true
            status = "Connecting Stream..."
            
            try {
                repo.listenToContacts().collect { newContacts ->
                    contacts = newContacts
                    status = "Connected (Stream: ${getCurrentTime()})"
                    loading = false
                }
            } catch (e: Throwable) {
                // Any error (Exception or Error) will cause an immediate exit
                System.err.println("FATAL: Sync stream failed: ${e.message}")
                e.printStackTrace()
                System.exit(1)
            }
        }

        // Input Loop
        LaunchedEffect(Unit) {
            while (true) {
                val rawCmd = inputProvider.nextCommand() ?: break
                
                // Handle Mouse Click Command internally
                val cmd = if (rawCmd.startsWith("CLICK_REMOVE ")) {
                    val idx = rawCmd.removePrefix("CLICK_REMOVE ").toIntOrNull()
                    if (idx != null && idx in contacts.indices) {
                        "d ${contacts[idx].id}" // Translate click to delete command
                    } else {
                        "NOOP"
                    }
                } else if (rawCmd.startsWith("DEBUG ")) {
                    debugMsg = rawCmd.removePrefix("DEBUG ")
                    "NOOP"
                } else {
                    rawCmd
                }

                if (cmd == "NOOP") continue
                lastCommand = cmd

                when {
                    cmd == "q" -> break
                    cmd == "r" -> {
                        // Refresh logic is now handled by stream, but manual refresh forces an immediate check?
                        // For simplicity, just update status.
                        status = "Manual Refresh Triggered"
                    }
                    cmd.startsWith("a ") -> {
                        val parts = cmd.substring(2).split(" ")
                        if (parts.size >= 2) {
                            loading = true
                            status = "Adding..."
                            repo.addContact(parts[0], parts[1])
                            // Stream will pick up the change
                            status = "Added ${parts[0]} (Waiting for stream)"
                            loading = false
                        }
                    }
                     cmd.startsWith("d ") -> {
                         val id = cmd.substring(2)
                         loading = true
                         status = "Deleting..."
                         repo.removeContact(id)
                         // Stream will pick up the change
                         status = "Deleted $id (Waiting for stream)"
                         loading = false
                     }
                }
            }
        }

        Column {
            // Line 1
            Text("Firebase KMP Address Book", color = Color.Green)
            // Line 2
            Text("--------------------------------------------------------------------------------")
            // Line 3
            Text("Commands: [a name email] Add, [d name] Delete, [r] Refresh, [q] Quit")
            // Line 4
            Row {
                Text("Status: ${status.padEnd(60)}")
                Text("[Close]", color = Color.Red)
            }
            // Line 5
            Text("Last Cmd: $lastCommand")
            // Line 6
            Text("--------------------------------------------------------------------------------")
            
            // Line 7 (Header)
            Row {
                Text("Name".padEnd(28), color = Color.Yellow)
                Text("Mail".padEnd(35), color = Color.Yellow)
                Text("Time".padEnd(16), color = Color.Yellow)
                Text("Operation", color = Color.Yellow)
            }
            // Line 8
            Text("-".repeat(89))

            // Line 9+ (Data)
            if (loading && contacts.isEmpty()) {
                Text("Loading data...", color = Color.Yellow)
            } else {
                contacts.forEach { contact ->
                    Row {
                        Text(contact.name.padEnd(28), color = Color.Cyan)
                        Text(contact.email.padEnd(35))
                        Text(contact.timestamp.take(16), color = Color.Blue)
                        Text("[Remove]".padStart(10), color = Color.Red)
                    }
                }
            }
            
            Text("--------------------------------------------------------------------------------")
            Text("> ", color = Color.Blue)
            Text("Debug: $debugMsg", color = Color.Blue)
        }
    }
}
