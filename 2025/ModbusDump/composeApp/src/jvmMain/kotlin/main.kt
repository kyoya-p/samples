package modbusdump

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import modbusdump.v2.MBRes
import modbusdump.v2.UI
import modbusdump.v2.read

fun main(args: Array<String>) {
    if (args.isNotEmpty()) modbusMain(args)
    else application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "ModbusDump",
            state = rememberWindowState(width = 1024.dp, height = 768.dp),
        ) {
            UI() //TODO
//            UI() //TODO
        }
    }
}


@OptIn(ExperimentalStdlibApi::class)
fun modbusMain(args: Array<String>) = runCatching {
    println("ModbusDump:")
    val hostAdr = args[0]
    val unitId = args[1].toInt()
    val modeCode = args[2].toInt()
    val regAdr = args.getOrElse(3) { "0" }.toInt()
    val regCount = args.getOrElse(4) { "1" }.toInt()
    val bulkSize = args.getOrElse(5) { "1" }.toInt()

    val master = ModbusTCPMaster(hostAdr)
    master.connect()
    master.read(
        AppData(
            hostAdr,
            mode = ReadType.entries.first { it.code == modeCode },
            unitId, regAdr, regCount, bulkSize
        )
    ) {
        println(it)
    }
//    master.read(AppData(hostAdr, unitId, regAdr, regCount, bulkSize)) { println(it) }
}.getOrElse { ex ->
    println(ex.message)
    println(
        $$"""
        usage: ModbusDump $hostAdr $unitId $mode $dataAddress $numberOfDataItem $bulkSize
        - hostAdr: target device address
        - unitId: sensor id (default=1)
        - mode: read mode
          - 1:READ_COILS,
          - 2:READ_INPUT_DISCRETES,
          - 3:READ_HOLDING_REGISTERS,
          - 4:READ_INPUT_REGISTERS
          - 13:READ_HOLDING_REGISTERS - Double word,
          - 14:READ_INPUT_REGISTERS - Double word,
        - dataAddress: First address of the data to be retrieved
        - numberOfDataItem: Number of data to be retrieved .
        - bulkSize: Number of data to be retrieved in one request.
        
    """.trimIndent()
    )
}

typealias ReadType = modbusdump.v2.ReadType
typealias MBRes = MBRes

@Serializable
data class AppData(
    val hostAdr: String = "",
    val mode: ReadType = ReadType.HOLDING_REGISTERS,
    val port: Int = 502,
    val unitId: Int = 1,
    val regAdr: Int = 0,
    val regCount: Int = 8,
    val nAcq: Int = 1,
    val nWord: Int = 1,
//    val result: String = "",
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
