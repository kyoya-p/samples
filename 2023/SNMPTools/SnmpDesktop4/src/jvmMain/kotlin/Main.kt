import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.reflect.KProperty

val app = AppProperties()

@OptIn(ExperimentalCoroutinesApi::class)
fun main() = application {
    var mibFileName by remember { mutableStateOf(app.mibFile ?: "No Data") }
    LaunchedEffect(app.mibFile) {
//        while (true) {
//            delay(200)
        mibFileName = app.mibFile ?: "No Data"
//        }
    }
    Window(
        onCloseRequest = ::exitApplication,
    ) { WinApp(window) }
}

class AppProperty(
    private val prop: Properties = Properties(),
    private val propFile: File = File("${System.getProperty("user.home")}/.snmpagent.properties"),
) {
    private fun <T> T.applyIf(t: Boolean, op: T.() -> Unit) = if (t) apply { op() } else this
    private val loadProp get() = prop.applyIf(propFile.exists()) { load(propFile.inputStream()) }
    private fun setAndStore(k: String, v: String?) =
        loadProp.apply { setProperty(k, v) }.store(propFile.outputStream(), "")

    operator fun getValue(r: Any?, p: KProperty<*>): String? = loadProp.getProperty(p.name)
    operator fun setValue(r: Any?, p: KProperty<*>, v: String?) = setAndStore(p.name, v)
}

class AppProperties {
    var ip by AppProperty()
    var port by AppProperty()
    var commStr by AppProperty()
    var mibFile by AppProperty()

    var ipRange by AppProperty()
}

class Logger(private val file: File = File(System.getenv("APPDATA"), "snmpdesktop_log.txt")) {
    val lastLines = ArrayDeque<String>()
    init {
        file.delete()
        lastLines.addLast("Logging ${file.path}\n")
    }

    operator fun plusAssign(s: String) {
        file.appendText(s)
        lastLines.addLast(s)
        if (lastLines.size > 50) lastLines.removeFirst()
    }
}