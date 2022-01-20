package jp.wjg.shokkaa.snmp4jutils

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.net.InetAddress

suspend fun main() {
    val transport = DefaultUdpTransportMapping()
    val snmp = Snmp(transport)
    //transport.listen()

    snmp.listen()
    broadcastFlow(snmp).collect {
        println(it.inetAddress.hostAddress)
    }
}

fun broadcastFlow(
    snmp: Snmp = Snmp(DefaultUdpTransportMapping()),
    pdu: PDU = PDU(PDU.GETNEXT, listOf(VariableBinding(OID(".1.3.6")))),
    target: CommunityTarget<UdpAddress> = CommunityTarget(
        UdpAddress(InetAddress.getByName("255.255.255.255"), 161),
        OctetString("public"),
    ),
) = callbackFlow {
    //val transport =
    //    DefaultUdpTransportMapping(UdpAddress(InetAddress.getByName("0.0.0.0"), 0))//送信元のポートを固定(firewall対策)
    //transport.listen()
    //val targetAddress = UdpAddress(InetAddress.getByName("255.255.255.255"), 161)
    //val targetAddress = UdpAddress(InetAddress.getByName("192.168.3.255"), 161)
    //val target = CommunityTarget<UdpAddress>(targetAddress, OctetString("public"))
    //target.version = SnmpConstants.version1
    //target.timeout = 2000
    //target.retries = 2
    //val vbl = listOf(VariableBinding(OID("1.3.6.1.2.1.1.1")))
    //val pdu = PDU(PDU.GETNEXT, vbl)
    snmp.send(pdu, target, null, object : ResponseListener {
        override fun <A : Address?> onResponse(event: ResponseEvent<A>?) {
            if (event == null || event.response == null) {
                close()
            } else {
                trySend(event.peerAddress as UdpAddress)
            }
        }
    })
    awaitClose { }
}