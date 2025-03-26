package jp.wjg.shokkaa.snmp4jutils.async

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.snmp4j.*
import org.snmp4j.Target
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.net.InetAddress
import java.util.concurrent.Semaphore
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


class SnmpAsync(val snmp: Snmp, val throttle: Duration = 0.seconds, val maxSession: Int = 0) : AutoCloseable {
    val sem = Semaphore(maxSession)

    override fun close() = snmp.close()

    companion object {
        fun createDefaultAgentTransport() =
            DefaultUdpTransportMapping(UdpAddress(InetAddress.getByName("0.0.0.0"), 161))

        fun createDefaultAgentSession() = Snmp(createDefaultAgentTransport()).async()
    }

    fun listenerWithMaxSession(cb: (ResponseEvent<UdpAddress>) -> Any?): ResponseListener = object : ResponseListener {
        override fun <A : Address?> onResponse(event: ResponseEvent<A>) {
            sem.release()
            (event.source as Snmp).cancel(event.request, this)
            @Suppress("UNCHECKED_CAST")
            cb(event as ResponseEvent<UdpAddress>)
        }
    }

}

suspend fun SnmpAsync.sendAsync(
    pdu: PDU,
    target: Target<UdpAddress>,
    userHandle: Any? = null
): ResponseEvent<UdpAddress> = suspendCoroutine { continuation ->
    snmp.send(pdu, target, userHandle, object : ResponseListener {
        override fun <A : Address> onResponse(r: ResponseEvent<A>?) {
            snmp.cancel(pdu, this)
            @Suppress("UNCHECKED_CAST")
            continuation.resume(r as ResponseEvent<UdpAddress>)
        }
    })
}

suspend fun SnmpAsync.uniCast(req: Request) = suspendCancellableCoroutine { continuation ->
    val listener = object : ResponseListener {
        override fun <A : Address> onResponse(r: ResponseEvent<A>) {
            runCatching {
                snmp.cancel(req.pdu, this)
                @Suppress("UNCHECKED_CAST")
                val res = when {
                    r.response == null -> Result.Timeout(r.userObject as Request)
                    else -> Result.Response(r.userObject as Request, r as SnmpEvent)
                }
                continuation.resume(res)
            }.onFailure { it.printStackTrace() }
            continuation.cancel()
        }
    }

    snmp.send(req.pdu, req.target, req, listener)
    continuation.invokeOnCancellation { snmp.cancel(req.pdu, listener) }
}

// [TODO]
suspend fun SnmpAsync.uniCast2(req: Request): Result = suspendCancellableCoroutine { continuation ->
    val listener: ResponseListener = object : ResponseListener {
        override fun <A : Address?> onResponse(event: ResponseEvent<A>) {
            (event.source as Snmp).cancel(event.request, this)
            val response = event.response
            val request = event.request
            if (response == null) {
                println("Request $request timed out")
                continuation.resume(Result.Timeout(req))
            } else {
                println("Received response $response on request $request")
                @Suppress("UNCHECKED_CAST")
                continuation.resume(Result.Response(req, event as SnmpEvent))
            }
        }
    }
    snmp.send(req.pdu, req.target, null, listener)
}


suspend fun SnmpAsync.getInetAddressByName(host: String) = suspendCoroutine<InetAddress> { continuation ->
    val adr = InetAddress.getByName(host)
    continuation.resume(adr)
}

suspend fun SnmpAsync.getUdpAddress(host: InetAddress, port: Int = 161) = suspendCoroutine { continuation ->
    continuation.resume(UdpAddress(host, port))
}

suspend fun SnmpAsync.getUdpAddress(host: String, port: Int = 161) = getUdpAddress(getInetAddressByName(host), port)

fun ByteArray.toIpV4Adr(): InetAddress = InetAddress.getByAddress(this)

@OptIn(ExperimentalUnsignedTypes::class)
fun UByteArray.toIpV4Adr(): InetAddress = InetAddress.getByAddress(toByteArray())

@OptIn(ExperimentalUnsignedTypes::class)
fun ULong.toUByteArray() = UByteArray(4) { i -> ((this shr ((3 - i) * 8)) and 0xffUL).toUByte() }

@OptIn(ExperimentalUnsignedTypes::class)
fun ULong.toIpV4Adr() = toUByteArray().toIpV4Adr()

@OptIn(ExperimentalUnsignedTypes::class)
fun ULong.toIpV4String() = toUByteArray().joinToString(".")
fun String.toIpV4Adr(): InetAddress = InetAddress.getByName(this)
fun String.toIpV4ULong() = trim().ifEmpty { throw Exception("Exception: Empty '$this' ") }.toIpV4Adr().toIpV4ULong()

@OptIn(ExperimentalUnsignedTypes::class)
fun InetAddress.toIpV4UByteArray() = address.toUByteArray()
fun InetAddress.toIpV4ULong() = address.fold(0UL) { a: ULong, e: Byte -> (a shl 8) + e.toUByte() }

@OptIn(ExperimentalUnsignedTypes::class)
fun InetAddress.toIpV4String() = toIpV4UByteArray().joinToString(".")

@Suppress("unused")
suspend fun SnmpAsync.send(pdu: PDU, target: Target<UdpAddress>, userHandle: Any? = null) =
    sendAsync(pdu, target, userHandle)

@Suppress("unused")
fun SnmpAsync.cancel(pdu: PDU, listener: ResponseListener) = apply { snmp.cancel(pdu, listener) }
fun SnmpAsync.listen() = apply { snmp.listen() }


fun Snmp.async() = SnmpAsync(this)

typealias SnmpTarget = CommunityTarget<UdpAddress>
typealias SnmpEvent = ResponseEvent<UdpAddress>

data class Request(val target: SnmpTarget, val pdu: PDU, val userData: Any? = null) {
    companion object
}

fun Request.Companion.from(adr: String, oid: String = ".1.3.6", userData: Any? = null) = Request(
    SnmpTarget(UdpAddress(InetAddress.getByName(adr), 161), OctetString("public")),
    PDU(PDU.GETNEXT, listOf(VariableBinding(OID(oid)))),
    userData
)

fun defaultTarget(ip: InetAddress) = SnmpTarget(UdpAddress(ip, 161), OctetString("public"))
fun defaultScanTarget(ip: InetAddress) = defaultTarget(ip).apply { timeout = 5000;retries = 1 }
fun defaultPDU() = PDU(PDU.GETNEXT, listOf(VariableBinding(OID(1, 3, 6))))
fun defaultRequest(ip: InetAddress) = Request(defaultTarget(ip), defaultPDU())

sealed class Result {
    data class Response(val request: Request, val received: SnmpEvent) : Result()
    data class Timeout(val request: Request) : Result()

    fun onResponse(op: (Response) -> Any?): Result = also { if (it is Response) op(it) }
    fun getResponseOrNull() = if (this is Response) received else null
    fun getValueOrNull(ix: Int) = getResponseOrNull()?.response?.variableBindings?.getOrNull(ix)?.variable
    fun onTimeout(op: (Timeout) -> Any?): Result = also { if (it is Timeout) op(it) }
}


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
fun String.toOid() = OID(this)

typealias ResponderEvent = CommandResponderEvent<UdpAddress>
typealias ResponseHandler = (ResponderEvent, PDU?) -> PDU?

@Suppress("unused")
@Serializable
data class Device(
    val ip: String,
    val vbl: List<@Contextual VariableBinding>,
)

fun createDefaultSenderSnmp() = SnmpBuilder().udp().v1().v3().build()
fun createDefaultSenderSnmpAsync() = createDefaultSenderSnmp().async()

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

