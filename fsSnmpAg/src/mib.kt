package mibtool

import kotlinx.serialization.*

@Serializable
data class SnmpConfig(
        val req: String = "walk",
        val comm: String = "public",
        val ver: String = "2c"
)

@Serializable
data class Request(
        val addr: String,
        val pdu: PDU,
        val snmpConfig: SnmpConfig,
)

@Serializable
data class Response(
        val addr: String,
        val pdu: PDU,
)

@Serializable
data class Target(
        val addr: String,
        val port: Int = 161,
        val credential: Credential = Credential(),
        val retries: Int = 5,
        val interval: Long = 5000,
)

@Serializable
data class Credential(
        val type: String = "2c",
        val v1commStr: String = "public",
        // v3...
)

@Serializable
data class PDU(
        val errSt: Int = 0,
        val errIdx: Int = 0,
        val type: Int = GETNEXT,
        val vbl: List<VB> = listOf<VB>(VB(".1")),
)

val PDU.Companion.GET: Int get() = -96
val PDU.Companion.GETNEXT: Int get() = -95
val PDU.Companion.RESPONSE: Int get() = -94
val PDU.Companion.SET: Int get() = -93

@Serializable
data class VB(
        val oid: String,
        val stx: Int = 0,
        val value: String = "",
)

val VB.Companion.ASN_UNIVERSAL get() = 0x00
val VB.Companion.ASN_APPLICATION get() = 0x40
val VB.Companion.OCTETSTRING get() = ASN_UNIVERSAL or 0x04
val VB.Companion.IPADDRESS get() = ASN_APPLICATION or 0x00

// from Simple OID File
fun String.toVB(): VB {

    fun String.dropWS() = dropWhile { it.isWhitespace() }
    fun String.takeNotWS() = takeWhile { !it.isWhitespace() }
    fun String.dropNotWS() = dropWhile { !it.isWhitespace() }

    val s1 = dropWS()
    val oid = s1.takeNotWS()

    val s2 = s1.dropNotWS().dropWS()
    val stx = s2.toInt()

    val s3 = s2.dropNotWS().dropWS()
    val value = s3.takeNotWS()

    val decValue = when (stx) {
        VB.OCTETSTRING, VB.IPADDRESS -> value.drop(1).dropLast(1)
        else -> value
    }
    return VB(oid, stx, decValue)
}
