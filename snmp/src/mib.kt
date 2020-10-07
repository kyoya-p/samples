package mibtool

import kotlinx.serialization.*
import org.snmp4j.smi.*

@Serializable
data class VB(
        val oid: String,
        val stx: Int?,
        val value: String?,
)

fun String.toVB(): VB {
    fun String.dropWS() = dropWhile { it.isWhitespace() }
    fun String.takeNotWS() = takeWhile { !it.isWhitespace() }
    fun String.dropNotWS() = dropWhile { !it.isWhitespace() }


    val oid = dropWS().takeNotWS()
    val stx = dropWS().dropNotWS().dropWS().takeNotWS().toInt()
    val value = dropWS().dropNotWS().dropWS().dropNotWS().dropWS().takeNotWS().drop(1).dropLast(1)
    return VB(oid, stx, value)
}
