@file:Suppress("unused")

package netSnmp

/*
 https://github.com/markabrahams/node-net-snmp
 */

//external fun require(module: String): dynamic

data class SnmpTarget(
    val addr: String,
    val port: Int = 161,
    val credential: Credential = Credential(),
    val retries: Int = 5,
    val interval: Long = 5000,

    val isBroadcast: Boolean? = false, // for discovery by broadcast
    val addrRangeEnd: String? = null, // for IP ranged discovery
)

data class Credential(
    val ver: String = "2c",
    val v1commStr: String = "public",
    // v3...
)

val GET: Int get() = -96
val GETNEXT: Int get() = -95
val RESPONSE: Int get() = -94
val SET: Int get() = -93

enum class SampleOID(val oid: String, val oidName: String) {
    sysDescr("1.3.6.1.2.1.1.1", "sysDescr"),
    sysObjectID("1.3.6.1.2.1.1.2", "sysObjectID"),
    sysName("1.3.6.1.2.1.1.5", "sysName"),
    sysLocation("1.3.6.1.2.1.1.6", "sysLocation"),

    hrDeviceDescr("1.3.6.1.2.1.25.3.2.1.3", "hrDeviceDescr"),
    hrDeviceID("1.3.6.1.2.1.25.3.2.1.4", "hrDeviceID"),
    hrDeviceStatus("1.3.6.1.2.1.25.3.2.1.5", "hrDeviceStatus"),
    hrPrinterStatus("1.3.6.1.2.1.25.3.5.1.1", "hrPrinterStatus"),
    hrPrinterDetectedErrorState("1.3.6.1.2.1.25.3.5.1.2", "hrPrinterDetectedErrorState"),

    prtGeneralPrinterName("1.3.6.1.2.1.43.5.1.1.16", "prtGeneralPrinterName"),
    prtGeneralSerialNumber("1.3.6.1.2.1.43.5.1.1.17", "prtGeneralSerialNumber"),
    prtInputVendorName("1.3.6.1.2.1.43.8.2.1.14", "prtInputVendorName"),
    prtOutputVendorName("1.3.6.1.2.1.43.9.2.1.8", "prtOutputVendorName"),
}

data class PDU(
    val type: Int = GETNEXT,
    val vbl: List<VB> = listOf(VB("1.3")),
    val errSt: Int = 0,
    val errIdx: Int = 0,
)

val ASN_UNIVERSAL get() = 0x00
val ASN_APPLICATION get() = 0x40
val NULL get() = ASN_UNIVERSAL or 0x05
val OCTETSTRING get() = ASN_UNIVERSAL or 0x04
val IPADDRESS get() = ASN_APPLICATION or 0x00

data class VB(
    val oid: String,
    val stx: Int = NULL,
    val value: String = "",
)


external fun require(module: String): dynamic

class Snmp {
    companion object {
        fun createSession(host: String, community: String): Session {
            val snmp = require("net-snmp")
            return Session(host, snmp.createSession(host = host, community = community))
        }
    }
}

data class VarBind(val oid: String, val type: Int, val value: Any) {
    companion object {
        fun from(vb: dynamic) = VarBind(vb.oid, vb.type, vb.value)
    }
}

class Session(val addr: String, private val session: dynamic) {
    fun getNext(oids: Array<String>, cb: (err: Int?, vbs: Array<VarBind>) -> Unit) = session.getNext(oids, cb)
    fun get(oids: Array<String>, cb: (error: Int?, vbs: Array<VarBind>) -> Unit) = session.get(oids, cb)
}

