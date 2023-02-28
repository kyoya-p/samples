import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.reflect.KProperty

val app = AppProperties()

@Composable
@Preview
fun AppX() {
    var text by remember { mutableStateOf("Hello, World!") }

    MaterialTheme {
        Button(onClick = {
            text = "Hello, Desktop!"
        }) {
            Text(text)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SNMP Desktop - ${app.mibFile ?: "No Data"}",
    ) { WinApp(window) }
}


class AppProperty(
    val prop: Properties = Properties(),
    val propFile: File = File("${System.getProperty("user.home")}/.snmpagent.properties"),
) {
    fun <T> T.applyIf(t: Boolean, op: T.() -> Unit) = if (t) apply { op() } else this
    val loadProp get() = prop.applyIf(propFile.exists()) { load(propFile.inputStream()) }
    fun setAndStore(k: String, v: String) = loadProp.apply { setProperty(k, v) }.store(propFile.outputStream(), "")
    operator fun getValue(r: Any?, p: KProperty<*>) = loadProp.getProperty(p.name)
    operator fun setValue(r: Any?, p: KProperty<*>, v: String) = setAndStore(p.name, v)
}

class AppProperties {
    var ip by AppProperty()
    var port by AppProperty()
    var commStr by AppProperty()
    var mibFile by AppProperty()

    var ipRange by AppProperty()
}

class Logger(val file: File = File("snmpdesktop_log.txt")) {
    init {
        file.delete()
    }

    val lastLines = ArrayDeque<String>()
    operator fun plusAssign(s: String) {
        file.appendText(s)
        lastLines.addLast(s)
        if (lastLines.size > 50) lastLines.removeFirst()
    }
}