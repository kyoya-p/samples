package jp.wjg.shokkaa.snmp4jutils

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.snmp4j.CommandResponderEvent
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.Target
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.smi.OID
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Suppress("unused")
class SnmpSuspendable(val snmp: Snmp) {
    suspend fun send(pdu: PDU, target: Target<UdpAddress>, userHandle: Any? = null) =
        suspendCoroutine<ResponseEvent<UdpAddress>> { continuation ->
            @Suppress("BlockingMethodInNonBlockingContext")
            snmp.send(pdu, target, userHandle, object : ResponseListener {
                override fun <A : org.snmp4j.smi.Address?> onResponse(r: ResponseEvent<A>?) {
                    snmp.cancel(pdu, this)
                    @Suppress("UNCHECKED_CAST")
                    continuation.resume(r as ResponseEvent<UdpAddress>)
                }
            })
            return@suspendCoroutine
        }

    suspend fun get(pdu: PDU, target: Target<UdpAddress>) = send(pdu.apply { type = PDU.GET }, target)
    suspend fun getNext(pdu: PDU, target: Target<UdpAddress>) = send(pdu.apply { type = PDU.GETNEXT }, target)

    @Suppress("unused")
    fun cancel(pdu: PDU, listener: ResponseListener) = snmp.cancel(pdu, listener)
    fun listen() = snmp.listen()
    fun close() = snmp.close()
}

fun Snmp.suspendable() = SnmpSuspendable(this)

@Suppress("EnumEntryName", "SpellCheckingInspection", "unused")
enum class SampleOID(val oid: OID, val oidName: String) {
    sysDescr(OID("1.3.6.1.2.1.1.1"), "sysDescr"),
    sysName(OID("1.3.6.1.2.1.1.5"), "sysName"),
    hrDeviceDescr(OID("1.3.6.1.2.1.25.3.2.1.3"), "hrDeviceDescr"),
    prtGeneralPrinterName(OID("1.3.6.1.2.1.43.5.1.1.16"), "prtGeneralPrinterName"),
    prtInputVendorName(OID("1.3.6.1.2.1.43.8.2.1.14"), "prtInputVendorName"),
    prtOutputVendorName(OID("1.3.6.1.2.1.43.9.2.1.8"), "prtOutputVendorName"),
}

fun OID(vararg ints: Int) = OID(ints)

typealias ResponderEvent = CommandResponderEvent<UdpAddress>
typealias ResponseHandler = (ResponderEvent, PDU?) -> PDU?

@Suppress("unused")
@Serializable
internal data class Device(
    val ip: String,
    val vbl: List<@Contextual VariableBinding>,
)