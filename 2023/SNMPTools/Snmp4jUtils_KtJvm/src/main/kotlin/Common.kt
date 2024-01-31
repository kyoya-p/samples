package jp.wjg.shokkaa.snmp4jutils.async

import jp.wjg.shokkaa.snmp4jutils.toIpv4Adr
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.snmp4j.*
import org.snmp4j.Target
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.OID
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
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
class SnmpAsync(val snmp: org.snmp4j.Snmp) : AutoCloseable {
    override fun close() = snmp.close()

    companion object {
        fun createDefaultAgentTransport() =
            DefaultUdpTransportMapping(UdpAddress(InetAddress.getByName("0.0.0.0"), 161))

        fun createDefaultAgentSession() = org.snmp4j.Snmp(createDefaultAgentTransport()).async()

    }
}


suspend fun SnmpAsync.sendAsync(
    pdu: PDU,
    target: Target<UdpAddress>,
    userHandle: Any? = null
): ResponseEvent<UdpAddress> {
//    yield()
    return suspendCoroutine<ResponseEvent<UdpAddress>> { continuation ->
        snmp.send(pdu, target, userHandle, object : ResponseListener {
            override fun <A : org.snmp4j.smi.Address> onResponse(r: ResponseEvent<A>?) {
                println("calledback ${(r?.request?.requestID?.toInt()?.toULong()?.toIpv4Adr())} ${r?.peerAddress}")
                snmp.cancel(pdu, this)
                @Suppress("UNCHECKED_CAST")
                continuation.resume(r as ResponseEvent<UdpAddress>)
            }
        })
    }
}

suspend fun SnmpAsync.getInetAddressByName(host: String) = suspendCoroutine<InetAddress> { continuation ->
    val adr = InetAddress.getByName(host)
    continuation.resume(adr)
}

suspend fun SnmpAsync.getUdpAddress(host: InetAddress, port: Int = 161) = suspendCoroutine<UdpAddress> { continuation ->
    continuation.resume(UdpAddress(host, port))
}

suspend fun SnmpAsync.getUdpAddress(host: String, port: Int = 161) = getUdpAddress(InetAddress.getByName(host), port)

@Suppress("unused")
suspend fun SnmpAsync.send(pdu: PDU, target: Target<UdpAddress>, userHandle: Any? = null) =
    sendAsync(pdu, target, userHandle)

@Suppress("unused")
fun SnmpAsync.cancel(pdu: PDU, listener: ResponseListener) = apply { snmp.cancel(pdu, listener) }
fun SnmpAsync.listen() = apply { snmp.listen() }


fun Snmp.async() = SnmpAsync(this)

typealias SnmpTarget = CommunityTarget<UdpAddress>

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

val defaultSenderSnmp get() = SnmpBuilder().udp().v1().v3().build().async()
