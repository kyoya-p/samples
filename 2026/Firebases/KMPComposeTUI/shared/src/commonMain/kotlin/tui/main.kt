package tui

import com.jakewharton.mosaic.runMosaic
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.layout.padding
import kotlinx.coroutines.runBlocking
import androidx.compose.runtime.*
import kotlinx.coroutines.delay

fun main(args: Array<String>) {
    println("Starting Mosaic TUI...")
    runBlocking {
        runMosaic {
            var count by remember { mutableStateOf(0) }

            Column(modifier = Modifier.padding(1)) {
                Text("Mosaic + Amper Demo", color = Color.Yellow)
                Text("-------------------")
                Row {
                    Text("Status: ")
                    Text(if (count < 20) "Running" else "Done", color = if (count < 20) Color.Green else Color.Red)
                }
                Text("Progress: [${"#".repeat(count)}${".".repeat(20 - count)}] $count/20")
            }

            LaunchedEffect(Unit) {
                while (count < 20) {
                    delay(150)
                    count++
                }
                delay(500)
            }
        }
    }
    println("Mosaic TUI finished.")
}