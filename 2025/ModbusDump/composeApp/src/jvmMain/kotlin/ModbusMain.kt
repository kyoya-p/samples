import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import kotlinx.coroutines.flow.flow

@OptIn(ExperimentalStdlibApi::class)
fun modbusMain(args: Array<String>) {
    val host = args[0]
    val unitId = args[1].toInt()
    val offset = args[2].toInt()
    val length = args[3].toInt()
    val windowSize = args[4].toInt()
    val master = ModbusTCPMaster(host)
    master.connect()
    master.modbusScan(unitId, offset, length, windowSize, ModbusMode.READ_HOLDING_REGISTERS).forEach {
        println(it)
    }
    master.disconnect()
}

fun ModbusTCPMaster.modbusScan(unitId: Int, start: Int, length: Int, windowSize: Int, mode: ModbusMode) = sequence {
    when (mode) {
        ModbusMode.READ_HOLDING_REGISTERS -> for (offset in start..<start + length step windowSize) {
            readMultipleRegisters(unitId, offset, windowSize).forEachIndexed { i, e ->
                yield(Record(offset + i, e.value))
            }
        }

        ModbusMode.READ_COILS -> for (offset in start..<start + length) {
            val v = readCoils(unitId, offset, length)?.let {
            }
        }

        else -> throw Exception("Unsupported mode: ${mode.name}")
    }

}

enum class ModbusMode {
    READ_COILS, READ_DISCRETE_INPUTS, READ_HOLDING_REGISTERS, READ_INPUT_REGISTERS
}

data class Record(val offset: Int, val data: Int)

fun Record.toText(): String {
    val v = data
    val c = (v shr 8).toPrintable() + (v and 0xff).toPrintable()
    val b = v.toString(2)
    return "$offset,${offset.toHexString().takeLast(4)},$v,${v.toHexString().takeLast(4)},\"$c\",$b"
}

fun Int.toPrintable() = if (this !in 0x20..<0x80 || this == ','.code) "<${toHexString().takeLast(2)}>"
else toChar().toString()

