package snmp4jWrapper

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import org.snmp4j.*
import org.snmp4j.Target
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.net.InetAddress


suspend fun main() {
    flowTest().collect {
        println(it.peerAddress)
    }
}

suspend fun flowTest() = callbackFlow<ResponseEvent<UdpAddress>> {
    val transport: TransportMapping<*> = DefaultUdpTransportMapping()

    Snmp(transport).use { snmp ->
        transport.listen()

        val pdu = PDU(PDU.GETNEXT, listOf(VariableBinding(OID(".1"))))
        val target1 = CommunityTarget<UdpAddress>(
                UdpAddress(InetAddress.getByName("10.36.102.245"), 161),
                OctetString("public")
        ).apply {
            retries = 2
            timeout = 1000
        }
        val target2 = CommunityTarget<UdpAddress>(
                UdpAddress(InetAddress.getByName("255.255.255.255"), 161),
                OctetString("public")
        ).apply {
            retries = 2
            timeout = 1000
        }

        val col1 = snmp.broadcastFlow(pdu, target2)
        val col2 = snmp.broadcastFlow(pdu, target2)

        val r1 = launch { col1.collect { offer(it) } }
        val r2 = launch { col2.collect { offer(it) } }
        r1.join()
        r2.join()
    }
    close()
    awaitClose()
}

fun Snmp.broadcastFlow(pdu: PDU, target: Target<UdpAddress>, userHandle: Object? = null) = callbackFlow<ResponseEvent<UdpAddress>> {
    val retries = target.retries
    target.retries = 0
    for (i in 0..retries) {
        sendFlow(pdu, target, userHandle).collect { offer(it) }
    }
    channel.close()
    awaitClose()
}

fun Snmp.sendFlow(pdu: PDU, target: Target<UdpAddress>, userHandle: Object? = null) = callbackFlow<ResponseEvent<UdpAddress>> {
    send(pdu, target, userHandle, object : ResponseListener {
        override fun <A : Address?> onResponse(event: ResponseEvent<A>) {
            val pdu = event.response
            if (pdu == null) {
                channel.close()
            } else {
                offer(event as ResponseEvent<UdpAddress>) // テンプレート型のコールバックはどうすればよいのでしょうね
            }
        }
    })
    awaitClose()
}



