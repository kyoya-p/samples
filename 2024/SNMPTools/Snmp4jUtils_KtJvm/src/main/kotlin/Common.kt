package jp.wjg.shokkaa.snmp4jutils.async

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.snmp4j.*
import org.snmp4j.Target
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.net.InetAddress
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalSerializationApi
fun main(args: Array<String>): Unit = runBlocking {
    SnmpBuilder().udp().v1().build().async().listen().use { snmp ->
        val tg = CommunityTarget(UdpAddress(InetAddress.getByName(args[0]), 161), OctetString("public"))
        val pdu = PDU(PDU.GETNEXT, listOf(VariableBinding(OID("1.3.6"))))
        val vbl = snmp.send(pdu, tg)
        println(vbl.response)
    }
}

@Suppress("unused")
class SnmpAsync(val snmp: Snmp) : AutoCloseable {
    override fun close() = snmp.close()

    companion object {
        fun createDefaultAgentTransport() =
            DefaultUdpTransportMapping(UdpAddress(InetAddress.getByName("0.0.0.0"), 161))

        fun createDefaultAgentSession() = Snmp(createDefaultAgentTransport()).async()

    }
}

suspend fun SnmpAsync.sendAsync(
    pdu: PDU,
    target: Target<UdpAddress>,
    userHandle: Any? = null
): ResponseEvent<UdpAddress> = suspendCoroutine<ResponseEvent<UdpAddress>> { continuation ->
    snmp.send(pdu, target, userHandle, object : ResponseListener {
        override fun <A : org.snmp4j.smi.Address> onResponse(r: ResponseEvent<A>?) {
            snmp.cancel(pdu, this)
            @Suppress("UNCHECKED_CAST")
            continuation.resume(r as ResponseEvent<UdpAddress>)
        }
    })
}

fun SnmpAsync.uniCast(tg: SnmpTarget, pdu: PDU, usr: Any? = null, rv: (SnmpEvent) -> Unit) =
    snmp.send(pdu, tg, usr, object : ResponseListener {
        override fun <A : org.snmp4j.smi.Address> onResponse(r: ResponseEvent<A>?) {
            snmp.cancel(pdu, this)
            @Suppress("UNCHECKED_CAST")
            rv(r as SnmpEvent)
        }
    })

suspend fun SnmpAsync.getInetAddressByName(host: String) = suspendCoroutine<InetAddress> { continuation ->
    val adr = InetAddress.getByName(host)
    continuation.resume(adr)
}

suspend fun SnmpAsync.getUdpAddress(host: InetAddress, port: Int = 161) = suspendCoroutine<UdpAddress> { continuation ->
    continuation.resume(UdpAddress(host, port))
}

suspend fun SnmpAsync.getUdpAddress(host: String, port: Int = 161) = getUdpAddress(getInetAddressByName(host), port)

fun ByteArray.toIpV4Adr() = InetAddress.getByAddress(this)
fun ULong.toIpV4Adr() = ByteArray(4) { i -> ((this shr ((3 - i) * 8)) and 0xffUL).toByte() }.toIpV4Adr()
fun String.toIpV4Adr() = InetAddress.getByName(this)
fun String.toIpV4ULong() = trim().toIpV4Adr().toIpV4ULong()

fun InetAddress.toIpV4ByteArray() = address
fun InetAddress.toIpV4ULong() = address.fold(0UL) { a: ULong, e: Byte -> (a shl 8) + e.toUByte() }
fun InetAddress.toIpV4String() = toIpV4ByteArray().joinToString(".")

@Suppress("unused")
suspend fun SnmpAsync.send(pdu: PDU, target: Target<UdpAddress>, userHandle: Any? = null) =
    sendAsync(pdu, target, userHandle)

@Suppress("unused")
fun SnmpAsync.cancel(pdu: PDU, listener: ResponseListener) = apply { snmp.cancel(pdu, listener) }
fun SnmpAsync.listen() = apply { snmp.listen() }


fun Snmp.async() = SnmpAsync(this)

typealias SnmpTarget = CommunityTarget<UdpAddress>
typealias SnmpEvent = ResponseEvent<UdpAddress>

@Suppress("EnumEntryName", "SpellCheckingInspection", "unused")
enum class SampleOID(val oid: String, val oidName: String) {
    sysDescr("1.3.6.1.2.1.1.1", "sysDescr"),
    sysName("1.3.6.1.2.1.1.5", "sysName"),
    hrDeviceDescr("1.3.6.1.2.1.25.3.2.1.3", "hrDeviceDescr"),
    hrDeviceID("1.3.6.1.2.1.25.3.2.1.4", "hrDeviceID"),
    hrDeviceStatus("1.3.6.1.2.1.25.3.2.1.5", "hrDeviceStatus"),
    prtGeneralPrinterName("1.3.6.1.2.1.43.5.1.1.16", "prtGeneralPrinterName"),
    prtInputVendorName("1.3.6.1.2.1.43.8.2.1.14", "prtInputVendorName"),
    prtOutputVendorName("1.3.6.1.2.1.43.9.2.1.8", "prtOutputVendorName"),
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

fun createDefaultSenderSnmpAsync() = SnmpBuilder().udp().v1().v3().build().async()

val sampleMibMap = sortedMapOf<OID, Variable>(
    OID(1, 3, 6, 1, 2, 1, 1, 1) to OctetString("Dummy SNMP Agent"),
    OID(1, 3, 6, 1, 2, 1, 1, 2) to OID(1, 3, 6, 1, 2, 1, 1, 1, 1, 1),
    OID(1, 3, 6, 1, 2, 1, 1, 3) to TimeTicks(77777777),
    OID(1, 3, 6, 1, 2, 1, 1, 4) to Integer32(65535),
    OID(1, 3, 6, 1, 2, 1, 1, 5) to OctetString("Dummy System"),
    OID(1, 3, 6, 9, 0) to OctetString("1.3.6.9.0"),
    OID(1, 3, 6, 9, 1, 2, 3) to OctetString("1.3.6.9.1.2.3")
)

@Suppress("unused")
val sampleMibList = sampleMibMap.map { VariableBinding(it.key, it.value) }

