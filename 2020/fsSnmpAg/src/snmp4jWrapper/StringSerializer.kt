package mibtool.snmp4jWrapper

import org.snmp4j.smi.*

fun VariableBinding.toString(): String {
    val o = oid.toString()
    val s = syntax.toString()
    val r = variable.toString()
    return "$o $s $r\n"
}

/*
fun Variable.toString(): String {
    return when (this) {
        is Integer32 -> toString()
        is OctetString -> this.value.caped()
        is Null -> ""
        is OID -> toString()
        is IpAddress -> inetAddress.address.caped()
        is Counter32 -> toString()
        is Gauge32 -> toString()
        is TimeTicks -> toString()
        is Opaque -> value.caped()
        is Counter64 -> toString()
        //128 -> Null(128)
        //129 -> Null(129)
        //130 -> Null(130)
        else -> throw IllegalArgumentException("Unsupported variable syntax: ${syntax}")
    }
}
*/

fun String.dropWS() = dropWhile { it.isWhitespace() }
fun String.takeNotWS() = takeWhile { !it.isWhitespace() }
fun String.dropNotWS() = dropWhile { !it.isWhitespace() }

fun String.toVariableBinding(): VariableBinding {
    val oid = dropWS().takeNotWS()
    val v = dropWS().dropNotWS().toVariable()
    return VariableBinding(OID(oid), v)
}


fun VariableBinding.toVBString(): String {
    fun ByteArray.toOctetString(): String {
        val os = this.joinToString("") {
            val b = it.toInt() and 0xff
            if (b <= 0x20 || 0x7f <= b || b == '\"'.toInt() || b == ':'.toInt()) ":%02x".format(b)
            else it.toChar().toString()
        }
        return "\"" + os + "\""
    }

    val v = variable
    val vr = when (v) {
        is Integer32 -> v.toString()
        is OctetString -> v.value.toOctetString()
        is Null -> "\"\""
        is OID -> v.toString()
        is IpAddress -> v.inetAddress.address.toOctetString()
        is Counter32 -> v.value.toString()
        is Gauge32 -> v.value.toString()
        is TimeTicks -> v.value.toString()
        is Opaque -> v.value.toString()
        is Counter64 -> v.value.toString()
        else -> throw IllegalArgumentException("Unsupported variable syntax: ${this.syntax}")
    }
    return "$oid $syntax $vr"
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
        4 -> OctetString(v.uncaped().toList().toByteArray())
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

fun String.uncaped() = generateSequence(0 to 0.toByte()) { (i, c) ->
    when {
        i >= length -> null
        this[i] == ':' -> (i + 3) to substring(i + 1, i + 3).toInt(16).toByte()
        else -> (i + 1) to this[i].toByte()
    }
}.drop(1).map { it.second }
