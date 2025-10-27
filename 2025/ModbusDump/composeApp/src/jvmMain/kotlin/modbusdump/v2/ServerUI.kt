package modbusdump.modbusdump.v3

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.darkrockstudios.libraries.mpfilepicker.MultipleFilePicker
import kotlinx.io.files.Path
import modbusdump.AppData
import modbusdump.ModbusSource


@Composable
fun AppData.ModServer(onChangeParams: (AppData) -> Unit) = Column {
//    LazyVerticalGrid(columns = GridCells.Fixed(4)) {
//        fun row(e: ModbusSource) {
//            item { Text(e.path) }
//            item { Text("${e.unitId}") }
//            item { Text("${e.listenPort}") }
//            item { Text("") }
//        }
//        this@ModServer.srcFiles.forEach {
//            row(it)
//        }
//    }

    // Tableを生成
    @Composable fun row(e: ModbusSource) {
        Row{
            Text(e.path)
            Text("${e.unitId}")
            Text("${e.listenPort}")
        }
    }
    this@ModServer.srcFiles.forEach {
        row(it)
    }


    TextButton(onClick = FileDialog {
        onChangeParams(copy(srcFiles = it.mapIndexed { i, e ->
            ModbusSource(e.toString(), 1 + i, 502 + i)
        }))
    }) { Text("FileDialog") }
}

@Composable
fun FileDialog(
    open: MutableState<Boolean> = remember { mutableStateOf(false) },
    onChange: (files: List<Path>) -> Unit
): () -> Unit {
    println("-:${open.value}")
    MultipleFilePicker(show = open.value, title = "Select Log files") { files ->
        open.value = false
        println("C:${open.value}")
        files?.let { onChange(files.map { Path(it.path) }) }
    }

    return {
        open.value = !open.value
        println("X:${open.value}")
    }
}
