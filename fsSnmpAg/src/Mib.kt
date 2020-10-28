package mibtool

import kotlinx.serialization.*
import java.net.InetAddress

@Serializable
data class ResponseEvent(
        val reqTarget: SnmpTarget,
        val reqPdu: PDU,
        val resTarget: SnmpTarget,
        val resPdu: PDU,
)

@Serializable
data class SnmpTarget(
        val addr: String,
        val port: Int = 161,
        val credential: Credential = Credential(),
        val retries: Int = 5,
        val interval: Long = 5000,

        val isBroadcast: Boolean = false, // for discovery by broadcast
        val addrRangeEnd: String? = null, // for IP ranged discovery
)

@Serializable
data class Credential(
        val ver: String = "2c",
        val v1commStr: String = "public",
        // v3...
)

@Serializable
data class PDU(
        val type: Int = GETNEXT,
        val vbl: List<VB> = listOf(VB(".1.3")),
        val errSt: Int = 0,
        val errIdx: Int = 0,
)

fun PDU.Companion.GET(vbl: List<VB>) = PDU(type = PDU.GET, vbl = vbl)
fun PDU.Companion.GETNEXT(vbl: List<VB>) = PDU(type = PDU.GETNEXT, vbl = vbl)
val PDU.Companion.GET: Int get() = -96
val PDU.Companion.GETNEXT: Int get() = -95
val PDU.Companion.RESPONSE: Int get() = -94
val PDU.Companion.SET: Int get() = -93

val PDU.Companion.sysDescr get() = ".1.3.6.1.2.1.1.1"
val PDU.Companion.sysObjectID get() = ".1.3.6.1.2.1.1.2"
val PDU.Companion.sysName get() = ".1.3.6.1.2.1.1.5"
val PDU.Companion.sysLocation get() = ".1.3.6.1.2.1.1.6"

val PDU.Companion.hrDeviceStatus get() = ".1.3.6.1.4.1.11.2.3.9.4.2.3.3.2.1.5"
val PDU.Companion.hrDeviceDescr get() = ".1.3.6.1.2.1.25.3.2.1.3"
val PDU.Companion.hrPrinterStatus get() = ".1.3.6.1.2.1.25.3.5.1.1"
val PDU.Companion.hrPrinterDetectedErrorState get() = ".1.3.6.1.2.1.25.3.5.1.2"
val PDU.Companion.prtGeneralSerialNumber get() = ".1.3.6.1.2.1.43.5.1.1.17"



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

