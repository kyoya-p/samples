import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster

@OptIn(ExperimentalStdlibApi::class)
fun modbusMain(args: Array<String>) {
    val target = args[0]
    val start = args[1].toInt()
    val length = args[2].toInt()
    val windowSize = args[3].toInt()
    val master = ModbusTCPMaster(target)
    master.connect()
    for (offset in start..<start + length step windowSize) {
        master.readMultipleRegisters(offset, windowSize)?.forEachIndexed { i, e ->
            val ix = offset + i
            val v = e.value
            val c = (v shr 8).toPrintable() + (v and 0xff).toPrintable()
            val b = v.toString(2)
            println("$ix,${ix.toHexString().takeLast(4)},$v,${v.toHexString().takeLast(4)},\"$c\",$b")
        }
    }
    master.disconnect()
}

data class Record(val address:Int, val data:Int)
fun Record.toText() =

//@OptIn(ExperimentalStdlibApi::class)
fun Int.toPrintable() = if (this !in 0x20..<0x80 || this == ','.code) "<${toHexString().takeLast(2)}>"
else toChar().toString()

