package mibtool.snmp4jWrapper

import kotlinx.serialization.*
import org.snmp4j.smi.VariableBinding
import org.snmp4j.PDU

//import org.snmp4j.smi.*

@Serializable
data class PDU(
        val id: Int,
        val errorStatus: Int,
        val errorIndex: Int,
        val type: Int,
        val vbl: List<VB>,
)

@Serializable
data class VB(
        val oid: String,
        val stx: Int?,
        val value: String?,
)

// from SNMP4J
fun PDU.toPDU() = PDU(
        id = requestID.value,
        errorStatus = errorStatus,
        errorIndex = errorIndex,
        type = type,
        vbl = variableBindings.map { it.toVB() }
)

fun VariableBinding.toVB() = VB(
        oid = oid.toOidString(),
        stx = syntax,
        value = toValueString(),
)


// from Simple OID File
fun String.toVB(): VB {
    val OCTETSTRING = 4
    fun String.dropWS() = dropWhile { it.isWhitespace() }
    fun String.takeNotWS() = takeWhile { !it.isWhitespace() }
    fun String.dropNotWS() = dropWhile { !it.isWhitespace() }

    val oid = dropWS().takeNotWS()
    val stx = dropWS().dropNotWS().dropWS().takeNotWS().toInt()
    val value = dropWS().dropNotWS().dropWS().dropNotWS().dropWS().takeNotWS()
    val decValue = if (stx == OCTETSTRING) value.drop(1).dropLast(1) else value
    return VB(oid, stx, decValue)
}
