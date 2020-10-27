package mibtool.snmp4jWrapper

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mibtool.SnmpTarget
import org.snmp4j.*
import org.snmp4j.Target
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.net.InetAddress
import kotlin.random.Random

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun main(): Unit = runBlocking {
    val pdu = mibtool.PDU()
    val startTarget = SnmpTarget("192.168.3.7").toSnmp4j()
    val endAddr = InetAddress.getByName("192.168.3.10")
    val broadcastTarget = SnmpTarget("255.255.255.255").toSnmp4j()
    snmpScopeDefault { snmp ->
        channelFlow {
            snmp.scanFlow(pdu.toSnmp4j(), startTarget, endAddr).collect { offer(it) }
            snmp.scanFlow(pdu.toSnmp4j(), startTarget, endAddr).collect { offer(it) }
            snmp.broadcastFlow(pdu.toSnmp4j(), broadcastTarget).collect { offer(it) }
        }.toList().distinctBy { it.peerAddress }
    }.forEach {
        println(it.peerAddress)
    }
}

var _reqId = Random.nextInt()
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


// TODO
fun Snmp.sendFlow(pdu: PDU, target: Target<UdpAddress>) = callbackFlow<ResponseEvent<UdpAddress>> {
    pdu.requestID = getGlobalRequestID()
    send(pdu, target, target, object : ResponseListener {
        override fun <A : Address?> onResponse(event: ResponseEvent<A>) {
            val pdu = event.response
            if (pdu == null) {
                close()
            } else {
                offer(event as ResponseEvent<UdpAddress>) // テンプレート型のコールバックはどう扱えば?
            }
        }
    })
    awaitClose()
}

fun Snmp.scanFlow(pdu: PDU, startTarget: Target<UdpAddress>, endAddr: InetAddress) = channelFlow {
    scanIpRange(startTarget.address.inetAddress, endAddr).map {
        launch {
            sendFlow(pdu.apply { requestID = getGlobalRequestID() }, SnmpTarget(it.hostAddress).toSnmp4j()).collect {
                offer(it)
            }
        }
    }.toList().forEach { it.join() }
    close()
    awaitClose()
}

suspend fun Snmp.broadcastFlow(pdu: PDU, target: Target<UdpAddress>) = callbackFlow<ResponseEvent<UdpAddress>> {
    val retries = target.retries
    target.retries = 0
    val detected = mutableSetOf<UdpAddress>()
    repeat(retries + 1) {
        sendFlow(pdu, target).collect {
            if (!detected.contains(it.peerAddress)) {
                detected.add(it.peerAddress)
                offer(it)
            }
        }
    }
    close()
    awaitClose()
}


