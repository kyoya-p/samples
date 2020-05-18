package mibtool

import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.TransportMapping
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.mp.SnmpConstants.version1
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
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


// TODO
// TODO
// TODO
// TODO
// TODO

fun main() {
    val transport: TransportMapping<*> = DefaultUdpTransportMapping()
    val snmp = Snmp(transport)
    transport.listen()

    val targetAddress: UdpAddress = UdpAddress(InetAddress.getByName("255.255.255.255"), 161)
    val target: CommunityTarget<UdpAddress> = CommunityTarget<UdpAddress>()
    target.setAddress(targetAddress)
    target.community = OctetString("public")
    target.timeout = 5_000
    target.retries = 5
    target.version = SnmpConstants.version2c

    val listener: ResponseListener = object : ResponseListener {
        override fun <A : Address?> onResponse(event: ResponseEvent<A>) {
            (event.source as Snmp).cancel(event.request, this)
            println("Received response PDU is: " + event.response)
            println("response listener thread id: " + Thread.currentThread().id)
            println("**********************************")
        }
    }

    println("main thread id: " + Thread.currentThread().id)
    val pdu1 = PDU()
    pdu1.add(VariableBinding(OID("1.3.6")))
    pdu1.type = PDU.GETNEXT
    snmp.send(pdu1, target, null, listener)

    Thread.sleep(30_000)
}

