package mibtool

import kotlinx.serialization.*

@Serializable
data class Request(
        val addr: String,
//        val target: SnmpTarget,

        val pdu: PDU,
)

@Serializable
data class ResponseEvent(
        val peerAddr: String,
        val pdu: PDU,
        val requestTarget: SnmpTarget?
)

@Serializable
data class SnmpTarget(
        val addr: String,
        val port: Int,
        val credential: Credential = Credential(),
        val retries: Int = 5,
        val interval: Long = 5000,
)

@Serializable
data class Credential(
        val ver: String = "2c",
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

fun PDU.Companion.GET(vbl: List<VB>) = PDU(type = PDU.GET, vbl = vbl)
fun PDU.Companion.GETNEXT(vbl: List<VB>) = PDU(type = PDU.GETNEXT, vbl = vbl)

val PDU.Companion.GET: Int get() = -96
val PDU.Companion.GETNEXT: Int get() = -95
val PDU.Companion.RESPONSE: Int get() = -94
val PDU.Companion.SET: Int get() = -93

@Serializable
data class VB(
        val oid: String,
        val stx: Int = VB.NULL,
        val value: String = "",
)

val VB.Companion.ASN_UNIVERSAL get() = 0x00
val VB.Companion.ASN_APPLICATION get() = 0x40
val VB.Companion.NULL get() = ASN_UNIVERSAL or 0x05
val VB.Companion.OCTETSTRING get() = ASN_UNIVERSAL or 0x04
val VB.Companion.IPADDRESS get() = ASN_APPLICATION or 0x00
