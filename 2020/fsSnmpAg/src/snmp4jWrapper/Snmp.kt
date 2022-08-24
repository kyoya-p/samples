package mibtool.snmp4jWrapper

import gdvm.device.SnmpTarget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.launch
import org.snmp4j.*
import org.snmp4j.Target
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.smi.*
import java.net.InetAddress
import kotlin.random.Random


var _reqId = Random.nextInt()
suspend fun getGlobalRequestID(): Integer32 {
    val mtx = Mutex()
    mtx.lock()
    val r = _reqId++
    mtx.unlock()
    return Integer32(r)
}

@ExperimentalCoroutinesApi
suspend fun Snmp.sendFlow(pdu: PDU, target: Target<UdpAddress>) = callbackFlow<ResponseEvent<UdpAddress>> {
    pdu.requestID = getGlobalRequestID()
    println("Target: ${target.address}")
    send(pdu, target, target, object : ResponseListener {
        override fun <A : Address?> onResponse(event: ResponseEvent<A>) {
            val resPdu = event.response
            if (resPdu == null) {
                close()
            } else {
                offer(event as ResponseEvent<UdpAddress>) // テンプレート型のコールバックはどう扱えば?
            }
        }
    })
    awaitClose()
}

@ExperimentalCoroutinesApi
fun Snmp.scanFlow(pdu: PDU, startTarget: Target<UdpAddress>, endAddr: InetAddress) = channelFlow {
    scanIpRange(startTarget.address.inetAddress, endAddr).map { addr ->
        val launch = launch {
            sendFlow(pdu.apply { requestID = getGlobalRequestID() }, SnmpTarget(addr.hostAddress).toSnmp4j()).collect {
                offer(it)
            }
        }
        launch
    }.toList().forEach { it.join() }
    close()
    awaitClose()
}


@ExperimentalCoroutinesApi
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


