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
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) =runBlocking {
    runMosaic {
        var count by remember { mutableStateOf(0) }

        Column(modifier = Modifier.padding(1)) {
            Text("Mosaic + Amper Demo", color = Color.Yellow)
            Text("-------------------")
            Row {
                Text("Count: $count")
            }
        }

        LaunchedEffect(Unit) {
            repeat(20) {
                count++
                delay(1.seconds)
            }
        }
    }
}