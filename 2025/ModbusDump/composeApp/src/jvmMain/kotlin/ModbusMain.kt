import com.ghgande.j2mod.modbus.Modbus
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster

@OptIn(ExperimentalStdlibApi::class)
fun modbusMain(args: Array<String>) = runCatching {
    val host = args[0]
    val unitId = args[1].toInt()
    val modeCode = args[2].toInt()
    val offset = args[3].toInt()
    val length = args[4].toInt()
    val windowSize = args[5].toInt()
    val master = ModbusTCPMaster(host)

    master.connect()
    val mode: ModbusMode =
        ModbusMode.entries.firstOrNull { it == modeCode } ?: throw Exception("Unknown mode: $modeCode")
    master.modbusScan(unitId, offset, length, windowSize, mode).forEach {
        println(it)
    }
    master.disconnect()
}.getOrElse { ex ->
    println(ex.message)
    println(
        """
        usage: ModbusDump hostAdr unitId mode ...
        - hostAdr: target device address
        - unitId: sensor id (default=1)
        - mode: read mode 
    """.trimIndent()
    )
}

fun ModbusTCPMaster.modbusScan(unitId: Int, start: Int, length: Int, windowSize: Int, mode: ModbusMode) = sequence {
    when (mode) {
        ModbusMode.READ_HOLDING_REGISTERS -> for (offset in start..<start + length step windowSize) {
            readMultipleRegisters(unitId, offset, windowSize).forEachIndexed { i, e ->
                yield(Record(offset + i, e.value))
            }
        }

        ModbusMode.READ_COILS -> for (offset in start..<start + length) {
            val v = readCoils(unitId, offset, length)?.let { bitVector ->
                bitVector.size()
            }
        }

        else -> throw Exception("Unsupported mode: ${mode.name}")
    }

}

enum class ModbusMode(code: Int, face: String) {
    READ_COILS(Modbus.READ_COILS, "1:READ_COILS"),
    READ_INPUT_DISCRETES(Modbus.READ_INPUT_DISCRETES, "2:READ_INPUT_DISCRETES"),
    READ_HOLDING_REGISTERS(Modbus.READ_HOLDING_REGISTERS, "3:READ_HOLDING_REGISTERS"),
    READ_INPUT_REGISTERS(Modbus.READ_INPUT_REGISTERS, "4:READ_INPUT_REGISTERS"),
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

