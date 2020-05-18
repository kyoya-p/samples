package  mibtool

import org.snmp4j.*
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.smi.*
import java.util.*


// TODO
// TODO
// TODO
// TODO
// TODO


fun Scanner.nextMIBSet(): TreeMap<OID, VariableBinding> {
    fun Scanner.nextVariable(): Variable {
        fun Scanner.nextOctetString(): ByteArray {
            next("\"(.*)\"")
            return Scanner(match().group(1)).run {
                generateSequence { true }.takeWhile { hasNext() }.map {
                    val b: Byte = findInLine(".").toCharArray()[0].toByte()
                    if (b == ':'.toByte()) findInLine("..").toInt(16).toByte() else b
                }.toList().toByteArray()
            }
        }

        return when (val valueType = nextInt()) {
            2 -> Integer32(nextInt())
            4 -> OctetString(nextOctetString())
            5 -> Null()
            6 -> OID(next())
            64 -> IpAddress(nextOctetString())
            65 -> Counter32(nextLong())
            66 -> Gauge32(nextLong())
            67 -> TimeTicks(nextLong())
            68 -> Opaque(nextOctetString())
            70 -> Counter64(nextLong())
            128 -> Null(128)
            129 -> Null(129)
            130 -> Null(130)
            else -> throw IllegalArgumentException("Unsupported variable syntax: ${valueType}")
        }
    }

    val size = nextInt()
    val m = TreeMap<OID, VariableBinding>()
    for (i in 0 until size) {
        val oid = OID(next())
        val variable = nextVariable()
        m[oid] = VariableBinding(oid, variable)
    }
    return m
}
