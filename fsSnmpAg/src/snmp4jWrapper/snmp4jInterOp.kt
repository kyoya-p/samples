package mibtool.snmp4jWrapper

import mibtool.ResponseEvent
import mibtool.SnmpTarget
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.mp.SnmpConstants.*
import org.snmp4j.smi.*
import java.net.InetAddress

fun ResponseEvent.Companion.from(res: org.snmp4j.event.ResponseEvent<UdpAddress>) = ResponseEvent(
        reqTarget = SnmpTarget.from(res.userObject as CommunityTarget<UdpAddress>),
        reqPdu = mibtool.PDU.from(res.request),

        resTarget = SnmpTarget.from(res.userObject as CommunityTarget<UdpAddress>),
        resPdu = mibtool.PDU.from(res.response),
)

fun SnmpTarget.Companion.from(t: CommunityTarget<UdpAddress>) = SnmpTarget(
        addr = t.address.inetAddress.hostAddress,
        port = t.address.port,
        credential = mibtool.Credential(
                ver = when (t.version) {
                    version1 -> "1"
                    version2c -> "2c"
                    version3 -> "3"
                    else -> ""
                },
                v1commStr = t.community.toString(),
        ),
        retries = t.retries,
        interval = t.timeout,
)

fun SnmpTarget.toSnmp4j() = CommunityTarget<UdpAddress>(
        UdpAddress(InetAddress.getByName(this.addr), 161),
        OctetString(this.credential.v1commStr),
)

fun mibtool.PDU.Companion.from(pdu: PDU) = mibtool.PDU(
        errSt = pdu.errorStatus,
        errIdx = pdu.errorIndex,
        type = pdu.type,
        vbl = pdu.variableBindings.map { it.toVB() }
)

fun mibtool.PDU.toSnmp4j() = org.snmp4j.PDU().also {
    it.type = this.type
    //it.requestID
    it.variableBindings = this.vbl.map { it.toSnmp4j() }
}


fun VariableBinding.toVB() = mibtool.VB(
        oid = oid.toOidString(),
        stx = syntax,
        value = toValueString(),
)

fun mibtool.VB.toSnmp4j() = org.snmp4j.smi.VariableBinding().also {
    it.oid = OID(this.oid)
    val v = value
    it.variable = when (stx) {
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

