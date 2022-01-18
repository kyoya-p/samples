package jp.wjg.shokkaa.snmp4jutils

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
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
    broadcastFlow(Snmp()).collect {
        println(it.inetAddress.hostAddress)
    }
}

fun broadcastFlow(snmp: Snmp) = callbackFlow {
    //val transport =
    //    DefaultUdpTransportMapping(UdpAddress(InetAddress.getByName("0.0.0.0"), 0))//送信元のポートを固定(firewall対策)
    //transport.listen()
    //val targetAddress = UdpAddress(InetAddress.getByName("255.255.255.255"), 161)
    val targetAddress = UdpAddress(InetAddress.getByName("192.168.3.8"), 161)
    val target = CommunityTarget<UdpAddress>(targetAddress, OctetString("public"))
    target.version = SnmpConstants.version1
    target.timeout = 5000
    val vbl = listOf(VariableBinding(OID("1.3.6.1.2.1.1.1")))

    snmp.send(PDU(PDU.GETNEXT, vbl), target, null, object : ResponseListener {
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