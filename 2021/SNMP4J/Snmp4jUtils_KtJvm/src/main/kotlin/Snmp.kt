import kotlinx.coroutines.cancel
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.Target
import org.snmp4j.event.ResponseEvent
import org.snmp4j.smi.UdpAddress
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SnmpSuspendable(val snmp: Snmp) {
    suspend fun send(pdu: PDU, target: Target<UdpAddress>) =
        suspendCoroutine<ResponseEvent<UdpAddress>> { continuation ->
            @Suppress("BlockingMethodInNonBlockingContext")
            snmp.send(pdu, target, null, object : org.snmp4j.event.ResponseListener {
                override fun <A : org.snmp4j.smi.Address?> onResponse(r: org.snmp4j.event.ResponseEvent<A>?) {
                    snmp.cancel(pdu, this)
                    @kotlin.Suppress("UNCHECKED_CAST")
                    continuation.resume(r as org.snmp4j.event.ResponseEvent<org.snmp4j.smi.UdpAddress>)
                }
            })
            return@suspendCoroutine
        }

    suspend fun get(pdu: PDU, target: Target<UdpAddress>) = send(pdu.apply { type = PDU.GET }, target)
    suspend fun getNext(pdu: PDU, target: Target<UdpAddress>) = send(pdu.apply { type = PDU.GETNEXT }, target)

    fun listen() = snmp.listen()
    fun close() = snmp.close()
}

fun Snmp.suspendable() = SnmpSuspendable(this)



