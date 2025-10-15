import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

fun main(args: Array<String>) {
    if (args.isNotEmpty()) modbusMain(args)
    else application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "ModbusDump",
            state = rememberWindowState(width = 1024.dp, height = 768.dp),
        ) {
            UI()
        }
    }
}

@Serializable
data class AppData(
    val hostAdr: String = "",
    val unitId: Int = 1,
    val regAdr: Int = 0,
    val regCount: Int = 8,
    val bulkSize: Int = 1,
    val mode: MBMode = MBMode.READ_HOLDING_REGISTERS,
    val result: String = "",
)

val appHome = Path("${System.getProperty("user.home")}/.modbusdump")
val configFile = Path("$appHome/config.json")
var config
    get() = runCatching {
        Json.decodeFromString<AppData>(SystemFileSystem.source(configFile).buffered().readString())
    }.getOrElse { AppData() }
    set(a) = with(SystemFileSystem) {
        if (!exists(configFile)) createDirectories(configFile.parent!!)
        sink(configFile).buffered().use { it.writeString(Json.encodeToString(a)) }
    }
