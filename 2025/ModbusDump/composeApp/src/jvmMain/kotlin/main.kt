import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

fun main(args: Array<String>) = if (args.isNotEmpty()) modbusMain(args)
else application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ModbusDump",
    ) { UI() }
}

@Serializable
data class AppData(
    val hostAdr: String = "",
    val unitId: Int = 0,
    val regAdr: Int = 0,
    val regCount: Int = 16,
    val bulkSize: Int = 1,
    val mode: ModbusMode = ModbusMode.READ_HOLDING_REGISTERS,
    val result: String = "",
)

val home = System.getProperty("user.home")!!
val configFile = Path("$home/.modbusdump/config.json")

var config: AppData
    get() = runCatching {
        Json.decodeFromString<AppData>(SystemFileSystem.source(configFile).buffered().readString())
    }.getOrElse { AppData() }
    set(a) = with(SystemFileSystem) {
        if (!exists(configFile)) createDirectories(configFile.parent!!)
        sink(configFile).buffered().use { it.writeString(Json.encodeToString(a)) }
    }
