import com.ghgande.j2mod.modbus.Modbus
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import com.ghgande.j2mod.modbus.procimg.Register
import com.ghgande.j2mod.modbus.util.BitVector
import kotlinx.serialization.Serializable
import modbusdump.AppData
import kotlin.Char

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
    master.read(AppData(hostAdr, unitId, regAdr, regCount, bulkSize)).forEach { println(it) }
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

fun BitVector.forEachIndexed(op: (Int, Boolean) -> Unit) = (0..<size()).forEach { op(it, getBit(it)) }
operator fun BitVector.get(i: Int) = if (getBit(i)) "1" else "0"
fun List<Register>.toLong() = fold(0L) { a, e -> a * 0x10000L + e.value }

fun ModbusTCPMaster.read(params: AppData) = with(params) {
    sequence {
        when (mode) {
            MBMode.READ_COILS -> for (ofs in regAdr..<regAdr + regCount step nAcq) {
                runCatching { readCoils(unitId, ofs, nAcq) }.onSuccess { bv ->
                    for (i in 0..<nAcq) yield("${(i + ofs).toAddress()}, ${bv[i]}")
                }.onFailure { ex -> yield("${ofs.toAddress()}, ${ex.message}") }
            }

            MBMode.READ_INPUT_DISCRETES -> for (ofs in regAdr..<regAdr + regCount step nAcq) {
                runCatching { readInputDiscretes(unitId, ofs, nAcq) }.onSuccess { bv ->
                    for (i in 0..<nAcq) yield("${(i + ofs).toAddress()}, ${bv[i]}")
                }.onFailure { ex -> yield("${ofs.toAddress()}, ${ex.message}") }
            }

            MBMode.READ_HOLDING_REGISTERS -> for (ofs in regAdr..<regAdr + regCount step nAcq) {
                runCatching { readMultipleRegisters(unitId, ofs, nAcq) }.onSuccess {
                    it.forEachIndexed { i, e -> yield("${(ofs + i).toAddress()}, ${e.value.toShort().toText()}") }
                }.onFailure { ex -> yield("${ofs.toAddress()}, ${ex.message}") }
            }

            MBMode.READ_INPUT_REGISTERS -> for (ofs in regAdr..<regAdr + regCount step nAcq) {
                runCatching { readInputRegisters(unitId, ofs, nAcq) }.onSuccess {
                    it.forEachIndexed { i, e -> yield("${(ofs + i).toAddress()}, ${e.value.toShort().toText()}") }
                }.onFailure { ex -> yield("${ofs.toAddress()}, ${ex.message}") }
            }

            MBMode.READ_HOLDING_REGISTERS_DWORD -> for (ofs in regAdr..<regAdr + regCount step ((nAcq + 1) / 2) * 2) {
                runCatching { readMultipleRegisters(unitId, ofs, ((nAcq + 1) / 2) * 2) }.onSuccess {
                    it.asSequence().chunked(2).forEachIndexed { i, e ->
                        val v = e.fold(0U) { a, e -> a * 0x10000U + e.value.toUInt() }.toInt()
                        yield("${(ofs + i * 2).toAddress()}, ${v.toText()}")
                    }
                }.onFailure { ex -> yield("${ofs.toAddress()}, ${ex.message}") }
            }

            MBMode.READ_INPUT_REGISTERS_DWORD -> for (ofs in regAdr..<regAdr + regCount step ((nAcq + 1) / 2) * 2) {
                runCatching { readInputRegisters(unitId, ofs, ((nAcq + 1) / 2) * 2) }.onSuccess {
                    it.asSequence().chunked(2).forEachIndexed { i, e ->
                        val v = e.fold(0U) { a, e -> a * 0x10000U + e.value.toUInt() }.toInt()
                        yield("${(ofs + i * 2).toAddress()}, ${v.toText()}")
                    }
                }.onFailure { ex -> yield("${ofs.toAddress()}, ${ex.message}") }
            }
        }
    }
}

enum class MBMode(
    val code: Int,
    val face: String,
) {
    READ_COILS(Modbus.READ_COILS, "1.Read Coils"),
    READ_INPUT_DISCRETES(Modbus.READ_INPUT_DISCRETES, "2.Read Input Discrete"),
    READ_HOLDING_REGISTERS(Modbus.READ_HOLDING_REGISTERS, "3.Read Holding Registers"),
    READ_INPUT_REGISTERS(Modbus.READ_INPUT_REGISTERS, "4.Read Input Registers"),
    READ_HOLDING_REGISTERS_DWORD(Modbus.READ_HOLDING_REGISTERS, "13.Read Holding Regs x2"),
    READ_INPUT_REGISTERS_DWORD(Modbus.READ_INPUT_REGISTERS, "14.Read Input Regs x2"),
}


fun Int.toAddress() = "${toString().padStart(5)}, ${toString(16).padStart(4, '0')}"

@OptIn(ExperimentalUnsignedTypes::class)
fun Short.toText(): String {
    val ba = (0..<1).scan(toUInt()) { a, _ -> a shr 8 }.map { it.toUByte() }.reversed().toUByteArray()
    val h = ba.joinToString("") { it.toString(16).padStart(2, '0') }
    val b = ba.joinToString("_") { it.toString(2).padStart(8, '0') }
    val s = ba.fold("") { a, e -> a + e.toByte().toPrintable() }
    val d = toString().padStart(2 * 5 / 2)
    val ud = toUShort().toString().padStart(2 * 5 / 2)
    return "$d, $ud, $h, $b, \"$s\""
}

@OptIn(ExperimentalUnsignedTypes::class)
fun Int.toText(words: Int = 1): String {
    val ba = (0..<2 * words - 1).scan(toUInt()) { a, _ -> a shr 8 }.map { it.toUByte() }.reversed().toUByteArray()
    val h = ba.joinToString("") { it.toString(16).padStart(2, '0') }
    val b = ba.joinToString("_") { it.toString(2).padStart(8, '0') }
    val s = ba.fold("") { a, e -> a + e.toByte().toPrintable() }
    val d = "$this".padStart(2 * words * 5 / 2)
    val ud = toUInt().toString().padStart(2 * words * 5 / 2)
    return "$d, $ud, $h, $b, \"$s\""
}

private fun Byte.toPrintable(): String = when {
    this < 0x20 || this == 0x7f.toByte() || this == ','.code.toByte() -> "\\x${toUByte().toString(16).padStart(2, '0')}"
    this == '\\'.code.toByte() -> "\\\\"
    else -> Char(toUShort()).toString()
}