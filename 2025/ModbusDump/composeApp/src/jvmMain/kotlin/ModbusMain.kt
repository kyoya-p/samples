import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import kotlinx.coroutines.flow.flow

@OptIn(ExperimentalStdlibApi::class)
fun modbusMain(args: Array<String>) {
    val host = args[0]
    val offset = args[1].toInt()
    val length = args[2].toInt()
    val windowSize = args[3].toInt()
    val master = ModbusTCPMaster(host)
    master.connect()
    master.modbusScan(offset, length, windowSize, ModbusMode.READ_HOLDING_REGISTERS).forEach {
//        println(it.toText())
        println(it)
    }
    master.disconnect()
}

fun ModbusTCPMaster.modbusScan(start: Int, length: Int, windowSize: Int, mode: ModbusMode) = sequence {
    for (offset in start..<start + length step windowSize) {
        readMultipleRegisters(offset, windowSize)?.forEachIndexed { i, e ->
            yield(Record(offset + i, e.value))
        }
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

