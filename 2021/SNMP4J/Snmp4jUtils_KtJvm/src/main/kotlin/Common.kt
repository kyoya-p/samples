import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.Target
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.smi.OID
import org.snmp4j.smi.UdpAddress
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SnmpSuspendable(val snmp: Snmp) {
    suspend fun send(pdu: PDU, target: Target<UdpAddress>) =
        suspendCoroutine<ResponseEvent<UdpAddress>> { continuation ->
            @Suppress("BlockingMethodInNonBlockingContext")
            snmp.send(pdu, target, null, object : ResponseListener {
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

@Suppress("EnumEntryName", "SpellCheckingInspection")
enum class SampleOID(val oid: OID, val oidName: String) {
    sysDescr(OID("1.3.6.1.2.1.1.1"), "sysDescr"),
    sysName(OID("1.3.6.1.2.1.1.5"), "sysName"),
    hrDeviceDescr(OID("1.3.6.1.2.1.25.3.2.1.3"), "hrDeviceDescr"),
    prtGeneralPrinterName(OID("1.3.6.1.2.1.43.5.1.1.16"), "prtGeneralPrinterName"),
    prtInputVendorName(OID("1.3.6.1.2.1.43.8.2.1.14"), "prtInputVendorName"),
    prtOutputVendorName(OID("1.3.6.1.2.1.43.9.2.1.8"), "prtOutputVendorName"),
}

fun OID.toMibString(): String {
    for (knownOid in SampleOID.values()) {
        if (startsWith(knownOid.oid)) {
            return knownOid.oidName + "." + value.drop(knownOid.oid.size()).joinToString(".")
        }
    }
    return toDottedString()
}

