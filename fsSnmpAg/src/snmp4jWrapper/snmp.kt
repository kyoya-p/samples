package mibtool.snmp4jWrapper

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.snmp4j.*
import org.snmp4j.Target
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.net.InetAddress
import java.util.*
import kotlin.random.Random


suspend fun main() = runBlocking {
    snmpScopeDefault { snmp ->
        val pdu = PDU(PDU.GETNEXT, listOf(VariableBinding(OID(".1.3"))))
        val targets = listOf("127.0.0.1", "255.255.255.255").map {
            CommunityTarget<UdpAddress>(
                    UdpAddress(InetAddress.getByName(it), 161),
                    OctetString("public")
            ).apply {
                timeout = 1000
                retries = 0
            }
        }
        snmp.scanFlow(pdu, targets[0], InetAddress.getByName("127.0.0.3")).collect {
            println(it.peerAddress)
        }
    }
}


var _reqId = Random.nextInt();
suspend fun getGlobalRequestID(): Integer32 {
    val mtx = Mutex()
    mtx.lock()
    val r = _reqId++
    mtx.unlock()
    return Integer32(r)
}

suspend fun <R> snmpScopeDefault(transport: TransportMapping<*> = DefaultUdpTransportMapping(), op: suspend (Snmp) -> R): R =
        Snmp(transport).use {
            transport.listen()
            op(it)
        }


suspend fun Snmp.sendFlow(pdu: PDU, target: Target<UdpAddress>, userHandle: Object? = null) = callbackFlow<ResponseEvent<UdpAddress>> {
    println("R= ${target.address}")
    if(false) /*TODO*/ send(pdu, target, userHandle, object : ResponseListener {
        override fun <A : Address?> onResponse(event: ResponseEvent<A>) {
            val pdu = event.response
            if (pdu == null) {
                close()
            } else {
                println("T= ${event.peerAddress}")
                offer(event as ResponseEvent<UdpAddress>) // テンプレート型のコールバックはどう扱えば?
            }
        }
    })
    close() /*TODO*/
    awaitClose()
}

suspend fun Snmp.scanFlow(pdu: PDU, target: Target<UdpAddress>, endAddr: InetAddress, userHandle: Object? = null) = callbackFlow<ResponseEvent<UdpAddress>> {
    val r = scanIpRange(target.address.inetAddress, endAddr).map { addr ->
        launch {
            val t2 = target
            t2.address.inetAddress = addr
            val p2 = pdu
            p2.requestID = getGlobalRequestID()
            println(t2)
            sendFlow(p2, t2, userHandle).collect { offer(it) }
        }
    }.toList().forEach { it.join() }
    close()
    awaitClose()
}

suspend fun Snmp.broadcastFlow(pdu: PDU, target: Target<UdpAddress>, userHandle: Any? = null) = callbackFlow<ResponseEvent<UdpAddress>> {
    val retries = target.retries
    target.retries = 0
    val detected = mutableSetOf<UdpAddress>()
    repeat(retries + 1) {
        sendFlow(pdu, target, userHandle as Object?).collect {
            if (!detected.contains(it.peerAddress)) {
                detected.add(it.peerAddress)
                offer(it)
            }
        }
    }
    close()
    awaitClose()
}


