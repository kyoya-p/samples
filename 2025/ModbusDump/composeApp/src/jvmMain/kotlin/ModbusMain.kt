import com.ghgande.j2mod.modbus.Modbus
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster

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
    val mode = ModbusMode.entries.first { it.code == modeCode }
    master.read(AppData(hostAdr, unitId, regAdr, regCount, bulkSize)).forEach {
        println(it)
    }
    master.disconnect()
}.getOrElse { ex ->
    println(ex.message)
    println(
        $$"""
        usage: ModbusDump $hostAdr $unitId mode ...
        - hostAdr: target device address
        - unitId: sensor id (default=1)
        - mode: read mode 1:READ_COILS, 2:READ_INPUT_DISCRETES, 3:READ_HOLDING_REGISTERS, 4:READ_INPUT_REGISTERS
        
        [mode=3/READ_HOLDING_REGISTERS]
        usage: ModbusDump $hostAdr $unitId 3 $dataAddress $dataLength $dataAcquisitionSize
        
    """.trimIndent()
    )
}

fun ModbusTCPMaster.read(params: AppData) = with(params) {
    sequence {
        when (mode) {
           ModbusMode.READ_COILS -> {
                val v = readCoils(unitId, regAdr, regCount)?.let { bitVector ->
                    for (i in 0..<bitVector.size()) {
                        val b = if(bitVector.getBit(i)) "1" else "0"
                        yield("$i,${i.toHexString().takeLast(4)},$b")
                    }
                }
            }
            ModbusMode.READ_INPUT_DISCRETES -> {
                val v = readInputDiscretes(unitId, regAdr, regCount)?.let { bitVector ->
                    for (i in 0..<bitVector.size()) {
                        val b = if(bitVector.getBit(i)) "1" else "0"
                        yield("$i,${i.toHexString().takeLast(4)},$b")
                    }
                }
            }

            ModbusMode.READ_HOLDING_REGISTERS -> for (offset in regAdr..<regAdr + regCount) {
                readMultipleRegisters(unitId, offset, bulkSize).forEachIndexed { i, e ->
                    yield(Record(offset + i, e.value).toText())
                }
            }
            ModbusMode.READ_INPUT_REGISTERS -> for (offset in regAdr..<regAdr + regCount) {
                readInputRegisters(unitId, offset, bulkSize).forEachIndexed { i, e ->
                    yield(Record(offset + i, e.value).toText())
                }
            }


            else -> throw Exception("Unsupported mode: ${mode.face}")
        }
    }
}

enum class ModbusMode(
    val code: Int,
    val face: String
) {
    READ_COILS(Modbus.READ_COILS, "1:READ_COILS"),
    READ_INPUT_DISCRETES(Modbus.READ_INPUT_DISCRETES, "2:READ_INPUT_DISCRETES"),
    READ_HOLDING_REGISTERS(Modbus.READ_HOLDING_REGISTERS, "3:READ_HOLDING_REGISTERS"),
    READ_INPUT_REGISTERS(Modbus.READ_INPUT_REGISTERS, "4:READ_INPUT_REGISTERS"), ;
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
