package modbusdump.v2

import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import com.ghgande.j2mod.modbus.util.BitVector
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import modbusdump.AppData
import modbusdump.appHome
import modbusdump.v2.ReadType.*

sealed class MBRes(val adr: Int, val message: String) {
    class OK(adr: Int, message: String) : MBRes(adr, message)
    class Error(adr: Int, message: String, val ex: Throwable) : MBRes(adr, message)
}

fun ModbusTCPMaster.read(params: AppData, cb: (MBRes) -> Unit): ResultCount = with(params) {
    val chunk = when (mode) {
        HOLDING_REGISTERS_X2, INPUT_REGISTERS_X2 -> 2
        else -> 1
    }
    val nAcqAligned = (nAcq + (chunk - 1)) / chunk * chunk
    var total = ResultCount(0, 0)
    val resData = mutableMapOf<Int, Int>()
    for (ofs in regAdr..<regAdr + regCount step nAcqAligned) {
        runCatching {
            when (mode) {
                COILS -> readCoils(unitId, ofs, nAcq)
                INPUT_DISCRETES -> readInputDiscretes(unitId, ofs, nAcq)
                else -> null
            }?.forEachIndexed { i, v ->
                cb(MBRes.OK(ofs, "$v"))
                resData[ofs + i] = if (v) 1 else 0
            }
            when (mode) {
                HOLDING_REGISTERS, HOLDING_REGISTERS_X2 -> readMultipleRegisters(unitId, ofs, nAcqAligned)
                INPUT_REGISTERS, INPUT_REGISTERS_X2 -> readInputRegisters(unitId, ofs, nAcqAligned)
                else -> null
            }?.run {
                asSequence().chunked(chunk).forEachIndexed { i, e ->
                    val v = e.fold(0U) { a, e -> a * 0x10000U + e.value.toUInt() }.toInt()
                    cb(MBRes.OK(ofs + i * chunk, "${ofs.toAddress()}, ${v.toText(chunk)}"))
                    resData[ofs + i] = v
                }
            }
            total = total.copy(total = total.total + 1)
        }.onFailure { ex ->
            total = total.copy(error = total.error + 1)
            cb(MBRes.Error(ofs, "${ofs.toAddress()}, ${ex.message}", ex))
        }
        SystemFileSystem.sink(Path("$appHome/data.mbus")).buffered().use { it.writeString(resData.toString()) }
    }
    return@with total
}

enum class ReadType(val code:Int, val face: String) {
    COILS(1, "1.Read Coils"),
    INPUT_DISCRETES(2, "2.Read Input Discretes"),
    HOLDING_REGISTERS(3, "3.Read Holding Registers"),
    HOLDING_REGISTERS_X2(23, "23.Read Holding Registers x2"),
    INPUT_REGISTERS(4, "4.Read Input Registers"),
    INPUT_REGISTERS_X2(24, "24.Read Input Registers x2"),
}

data class ModbusDataSet(
    val readType: ReadType,
    val dataSet: Map<Int, Int>
)

data class ResultCount(val total: Int, val error: Int)


fun BitVector.forEachIndexed(op: (Int, Boolean) -> Unit) = (0..<size()).forEach { op(it, getBit(it)) }
operator fun BitVector.get(i: Int) = if (getBit(i)) "1" else "0"
fun Int.toAddress() = "${toString().padStart(5)}, ${toString(16).padStart(4, '0')}"


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