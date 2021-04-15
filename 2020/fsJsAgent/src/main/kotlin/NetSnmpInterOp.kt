package netSnmp

import kotlinx.serialization.Serializable

/*
 https://github.com/markabrahams/node-net-snmp
 */

@Serializable
data class SnmpTarget(
    val addr: String,
    val port: Int = 161,
    val credential: Credential = Credential(),
    val retries: Int = 5,
    val interval: Long = 5000,

    val isBroadcast: Boolean? = false, // for discovery by broadcast
    val addrRangeEnd: String? = null, // for IP ranged discovery
)

@Serializable
data class Credential(
    val ver: String = "2c",
    val v1commStr: String = "public",
    // v3...
)

val GET: Int get() = -96
val GETNEXT: Int get() = -95
val RESPONSE: Int get() = -94
val SET: Int get() = -93

val sysDescr get() = "1.3.6.1.2.1.1.1"
val sysObjectID get() = "1.3.6.1.2.1.1.2"
val sysName get() = "1.3.6.1.2.1.1.5"
val sysLocation get() = "1.3.6.1.2.1.1.6"

val hrDeviceStatus get() = "1.3.6.1.4.1.11.2.3.9.4.2.3.3.2.1.5"
val hrDeviceDescr get() = "1.3.6.1.2.1.25.3.2.1.3"
val hrPrinterStatus get() = "1.3.6.1.2.1.25.3.5.1.1"
val hrPrinterDetectedErrorState get() = "1.3.6.1.2.1.25.3.5.1.2"
val prtGeneralSerialNumber get() = "1.3.6.1.2.1.43.5.1.1.17"


@Serializable
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

@Serializable
data class VB(
    val oid: String,
    val stx: Int = NULL,
    val value: String = "",
)


external fun require(module: String): dynamic

class Snmp() {
    companion object {
        fun createSession(host: String, community: String): Session {
            val snmp = require("net-snmp")
            return Session(snmp.createSession(host = host, community = community))
        }
    }
}

data class VarBind(val oid: String, val type: Int, val value: Any) {
    companion object {
        fun from(vb: dynamic) = VarBind(vb.oid, vb.type, vb.value)
    }
}

class Session(private val session: dynamic) {
    fun getNext(oids: Array<String>, callback: (error: Int?, varbinds: Array<VarBind>) -> Unit) =
        session.getNext(oids, callback)

    fun get(oids: Array<String>, callback: (error: Int?, varbinds: Array<VarBind>) -> Unit) =
        session.get(oids, callback)
}

