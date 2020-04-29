package mibtool

import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.io.PrintStream
import java.net.InetAddress


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


fun main() {
    val r: PDU = Snmp(DefaultUdpTransportMapping()).use { snmp ->
        val pdu = PDU(PDU.GETNEXT, listOf(VariableBinding(OID(".1.3.6"))))
        //val addr = InetAddress.getByName("10.36.102.45")
        val addr = InetAddress.getByName("255.255.255.255")
        val target = CommunityTarget(
                UdpAddress(addr, 161)
                , OctetString("public"))
        snmp.listen()
        snmp.send(pdu, target).response!!
    }
    //println(r.)
    println(r.variableBindings[0])
}