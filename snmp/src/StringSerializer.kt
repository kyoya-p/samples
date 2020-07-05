package  mibtool

import org.snmp4j.smi.*

fun VariableBinding.toString(): String {
    val o = oid.toString()
    val s = syntax.toString()
    val r = variable.toString()
    return "$o $s $r\n"
}

fun String.dropWS() = dropWhile { it.isWhitespace() }
fun String.takeNotWS() = takeWhile { !it.isWhitespace() }
fun String.dropNotWS() = dropWhile { !it.isWhitespace() }

fun String.toVariableBinding(): VariableBinding {
    val oid = dropWS().takeNotWS()
    val v = dropWS().dropNotWS().toVariable()
    return VariableBinding(OID(oid), v)
}

fun String.toVariable(): Variable {
    val stx = dropWS().takeNotWS().toInt()
    val v = dropWS().dropNotWS().dropWS().run {
        when (this[0]) {
            '\"' -> drop(1).takeWhile { it != '\"' }
            else -> takeNotWS()
        }
    }
    //println("$stx $v [${v.uncaped().map{it.toUByte().toString(16)}.joinToString()}]")
    return when (stx) {
        2 -> Integer32(v.toInt())
        4 -> OctetString(v)
        5 -> Null()
        6 -> OID(v)
        64 -> IpAddress(v.uncaped().toList().toByteArray())
        65 -> Counter32(v.toLong())
        66 -> Gauge32(v.toLong())
        67 -> TimeTicks(v.toLong())
        68 -> Opaque(v.toByteArray())
        70 -> Counter64(v.toLong())
        128 -> Null(128)
        129 -> Null(129)
        130 -> Null(130)
        else -> throw IllegalArgumentException("Unsupported variable syntax: ${stx}")
    }
}