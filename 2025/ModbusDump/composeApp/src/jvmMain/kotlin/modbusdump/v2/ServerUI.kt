package modbusdump.modbusdump.v3

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.MultipleFilePicker
import kotlinx.io.files.Path
import modbusdump.AppData
import modbusdump.ModbusSource


@Composable
fun AppData.ModServer(onChangeParams: (AppData) -> Unit) = Column {
    @Composable
    fun row(e: ModbusSource, onChange: (ModbusSource) -> Unit) {
        @Composable
        fun Cell(s: String, w: Int? = null, onChange: (String) -> Unit) = TextField(
            s, modifier = w?.let { Modifier.width(it.dp) } ?: Modifier.weight(1f),
            isError = false, singleLine = true, onValueChange = onChange
        )
        Row {
            Cell(e.path) { onChange(e.copy(path = it)); println(it) }
            TextField("${e.unitId}", modifier = Modifier.width(40.dp), onValueChange = {})
            TextField("${e.listenPort}", modifier = Modifier.width(70.dp), onValueChange = {})
        }
    }
    this@ModServer.srcFiles.forEachIndexed { i, e ->
        row(e) { onChangeParams(copy(srcFiles = srcFiles.toMutableList().apply { set(i, it) })) }
    }

    TextButton(onClick = FileDialog {
        onChangeParams(copy(srcFiles = it.mapIndexed { i, e ->
            ModbusSource(e.toString(), 1 + i, 502 + i)
        }))
    }) { Text("Select Source Files") }
}

@Composable
fun FileDialog(
    open: MutableState<Boolean> = remember { mutableStateOf(false) },
    onChange: (files: List<Path>) -> Unit
): () -> Unit {
    MultipleFilePicker(show = open.value, title = "Select Log files") { files ->
        open.value = false
        files?.let { onChange(files.map { Path(it.path) }) }
    }
    return { open.value = !open.value }
}
