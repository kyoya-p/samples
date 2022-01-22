package jp.wjg.shokkaa.snmp4jutils

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.net.InetAddress

suspend fun main() {
    broadcastFlow().collect {
        println(it.inetAddress.hostAddress)
    }
}


fun broadcastFlow(
    snmp: Snmp = Snmp(DefaultUdpTransportMapping()).apply { listen() },
    adr: String = "255.255.255.255",
    oid: OID = OID(".1.3.6"),
) = broadcastFlow(
    snmp = snmp,
    pdu = PDU(PDU.GETNEXT, listOf(VariableBinding(oid))),
    target = CommunityTarget(UdpAddress(java.net.InetAddress.getByName(adr), 161), OctetString("public"))
)

fun broadcastFlow(
    snmp: Snmp = Snmp(DefaultUdpTransportMapping()).apply { listen() },
    target: CommunityTarget<UdpAddress>,
    pdu: PDU,
) = callbackFlow {
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