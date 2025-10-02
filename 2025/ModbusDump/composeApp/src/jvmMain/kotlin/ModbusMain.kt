import com.ghgande.j2mod.modbus.Modbus
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import kotlinx.coroutines.suspendCancellableCoroutine

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

//suspend fun x() = suspendCancellableCoroutine { cont ->
//
//}

fun ModbusTCPMaster.read(params: AppData) = with(params) {
    sequence {
        when (mode) {
            ModbusMode.READ_COILS -> {
                readCoils(unitId, regAdr, regCount)?.let { bv ->
                    for (i in 0..<bv.size()) {
                        val b = if (bv.getBit(i)) "1" else "0"
                        yield("$i,${i.toHexString().takeLast(4)},$b")
                    }
                }
            }

            ModbusMode.READ_INPUT_DISCRETES -> {
                readInputDiscretes(unitId, regAdr, regCount)?.let { bv ->
                    for (i in 0..<bv.size()) {
                        val b = if (bv.getBit(i)) "1" else "0"
                        yield("$i,${i.toHexString().takeLast(4)},$b")
                    }
                }
            }

            ModbusMode.READ_HOLDING_REGISTERS -> for (offset in regAdr..<regAdr + regCount step bulkSize) {
                runCatching { readMultipleRegisters(unitId, offset, bulkSize) }.onSuccess {
                    it.forEachIndexed { i, e -> yield(Record(offset + i, e.value).toText()) }
                }.onFailure { yield("$offset, ${offset.toHex4()}, ${it.message}") }
            }

            ModbusMode.READ_INPUT_REGISTERS -> for (offset in regAdr..<regAdr + regCount step bulkSize) {
                runCatching { readInputRegisters(unitId, offset, bulkSize) }.onSuccess {
                    it.forEachIndexed { i, e -> yield(Record(offset + i, e.value).toText()) }
                }.onFailure { yield("$offset, ${offset.toHex4()}, ${it.message}") }
            }
        }
    }
}

enum class ModbusMode(
    val code: Int,
    val face: String
) {
    READ_COILS(Modbus.READ_COILS, "1.Read Coils"),
    READ_INPUT_DISCRETES(Modbus.READ_INPUT_DISCRETES, "2.Read Input Discretes"),
    READ_HOLDING_REGISTERS(Modbus.READ_HOLDING_REGISTERS, "3.Read Holding Registers"),
    READ_INPUT_REGISTERS(Modbus.READ_INPUT_REGISTERS, "4.Read Input Registers"),
}

data class Record(val offset: Int, val data: Int)

fun Int.toHex4() = toHexString().takeLast(4)
fun Record.toText(): String {
    val v = data
    val c = (v shr 8).toPrintable() + (v and 0xff).toPrintable()
    val b = v.toString(2)
    return "$offset,${offset.toHex4()},$v,${v.toHex4()},\"$c\",$b"
}

fun Int.toPrintable() = if (this !in 0x20..<0x80 || this == ','.code) "<${toHexString().takeLast(2)}>"
else toChar().toString()
