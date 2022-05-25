@file:Suppress("unused")

package jp.wjg.shokkaa.snmp4jutils

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.snmp4j.*
import org.snmp4j.Target
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.smi.OID
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Suppress("unused")
class SnmpSuspendable(val snmp: Snmp)

suspend fun SnmpSuspendable.sendAsync(pdu: PDU, target: Target<UdpAddress>, userHandle: Any? = null) =
    suspendCoroutine<ResponseEvent<UdpAddress>> { continuation ->
        print(pdu)
        snmp.send(pdu, target, userHandle, object : ResponseListener {
            override fun <A : org.snmp4j.smi.Address?> onResponse(r: ResponseEvent<A>?) {
                println(" => ${r?.response}")
                snmp.cancel(pdu, this)
                @Suppress("UNCHECKED_CAST")
                continuation.resume(r as ResponseEvent<UdpAddress>)
            }
        })
        return@suspendCoroutine
    }

@Suppress("unused")
suspend fun SnmpSuspendable.send(pdu: PDU, target: Target<UdpAddress>, userHandle: Any? = null) =
    sendAsync(pdu, target, userHandle)

@Suppress("unused")
fun SnmpSuspendable.cancel(pdu: PDU, listener: ResponseListener) = apply { snmp.cancel(pdu, listener) }
fun SnmpSuspendable.listen() = apply { snmp.listen() }
fun SnmpSuspendable.close() = apply { snmp.close() }


fun Snmp.suspendable() = SnmpSuspendable(this)

typealias SnmpTarget = CommunityTarget<UdpAddress>

@Suppress("EnumEntryName", "SpellCheckingInspection", "unused")
enum class SampleOID(val oid: OID, val oidName: String) {
    sysDescr(OID("1.3.6.1.2.1.1.1"), "sysDescr"),
    sysName(OID("1.3.6.1.2.1.1.5"), "sysName"),
    hrDeviceDescr(OID("1.3.6.1.2.1.25.3.2.1.3"), "hrDeviceDescr"),
    hrDeviceID(OID("1.3.6.1.2.1.25.3.2.1.4"), "hrDeviceID"),
    hrDeviceStatus(OID("1.3.6.1.2.1.25.3.2.1.5"), "hrDeviceStatus"),
    prtGeneralPrinterName(OID("1.3.6.1.2.1.43.5.1.1.16"), "prtGeneralPrinterName"),
    prtInputVendorName(OID("1.3.6.1.2.1.43.8.2.1.14"), "prtInputVendorName"),
    prtOutputVendorName(OID("1.3.6.1.2.1.43.9.2.1.8"), "prtOutputVendorName"),
}

fun OID(vararg ints: Int) = OID(ints)

typealias ResponderEvent = CommandResponderEvent<UdpAddress>
typealias ResponseHandler = (ResponderEvent, PDU?) -> PDU?

@Suppress("unused")
@Serializable
data class Device(
    val ip: String,
    val vbl: List<@Contextual VariableBinding>,
)