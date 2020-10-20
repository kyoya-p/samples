package snmp4jWrapper

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import org.snmp4j.*
import org.snmp4j.Target
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.net.InetAddress
import kotlin.random.Random


suspend fun main() {
     snmpScopeDefault { snmp ->
        val pdu = PDU(PDU.GETNEXT, listOf(VariableBinding(OID(".1.3"))))
        val targets = listOf("127.0.0.1", "255.255.255.255").map {
            CommunityTarget<UdpAddress>(
                    UdpAddress(InetAddress.getByName(it), 161),
                    OctetString("public")
            )
        }
        snmp.sendFlow(pdu, targets[0]).collect {
            println(it.peerAddress)
        }
    }
}


var _reqId = Random.nextInt();
suspend fun Snmp.getGlobalRequestID(): Int {
    val mtx = Mutex()
    mtx.lock()
    val r = _reqId++
    mtx.unlock()
    return r
}

suspend fun <R> snmpScopeDefault(op: suspend (Snmp) -> R): R {
    val transport: TransportMapping<*> = DefaultUdpTransportMapping()
    return Snmp(transport).use {
        transport.listen()
        op(it)
    }
}

suspend fun Snmp.sendFlow(pdu: PDU, target: Target<UdpAddress>, userHandle: Object? = null) = callbackFlow<ResponseEvent<UdpAddress>> {
    pdu.requestID = Integer32(getGlobalRequestID())
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

suspend fun Snmp.broadcastFlow(pdu: PDU, target: Target<UdpAddress>, userHandle: Object? = null) = callbackFlow<ResponseEvent<UdpAddress>> {
    val retries = target.retries
    target.retries = 0
    val detected = mutableSetOf<UdpAddress>()
    for (i in 0..retries) {
        sendFlow(pdu, target, userHandle).collect {
            if (!detected.contains(it.peerAddress)) {
                detected.add(it.peerAddress)
                offer(it)
            }
        }
    }
    close()
    awaitClose()
}


