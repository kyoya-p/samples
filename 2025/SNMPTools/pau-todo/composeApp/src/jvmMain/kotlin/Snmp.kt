@file:OptIn(ExperimentalUnsignedTypes::class)

package jp.wjg.shokkaa.snmp

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.net.InetAddress
import kotlin.coroutines.resume
import kotlin.time.Clock.System.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

val defaultSenderSnmp by lazy { createDefaultSenderSnmp() }
fun createDefaultSenderSnmp(bufferSizeByte: Int = 1024 * 1024): Snmp {
    val trasport = DefaultUdpTransportMapping()
    trasport.receiveBufferSize = bufferSizeByte
    return SnmpBuilder().udp(UdpAddress()).tm(trasport).v1().v3().build()!!
}

typealias SnmpTarget = CommunityTarget<UdpAddress>
typealias SnmpEvent = ResponseEvent<UdpAddress>

data class Request(val target: SnmpTarget, val pdu: PDU, val userData: Any? = null)

fun Request(
    strAdr: String = "127.0.0.1",
    inetAdr: InetAddress = InetAddress.getByName(strAdr),
    port: Int = 161,
    udpAdr: UdpAddress = UdpAddress(inetAdr, port),

    reqType: Int = PDU.GETNEXT,

    commStrV1: String = "public",
    comm: OctetString = OctetString(commStrV1),

    nRetry: Int = 5,
    interval: Duration = 5.seconds,
    intervalMs: Long = interval.inWholeMilliseconds,
    target: SnmpTarget = SnmpTarget(udpAdr, comm).apply { retries = nRetry; timeout = intervalMs },

    strOid: String = ".1.3.6",
    strOids: List<String> = listOf(strOid),
    oids: Array<OID> = strOids.map { OID(it) }.toTypedArray(),
    vbl: List<VariableBinding> = oids.map { VariableBinding(it) },
    reqId: UInt? = null,
    pdu: PDU = PDU(reqType, vbl).apply { reqId?.let { reqId -> requestID = Integer32(reqId.toInt()) } },

    userData: Any? = null
) = Request(target = target, pdu = pdu, userData = userData)

fun PDU(
    type: Int = PDU.GETNEXT,
    strOid: String? = null,
    strOids: List<String> = listOfNotNull(strOid),
    oids: Array<OID> = strOids.map { OID(it) }.toTypedArray(),
    vbl: List<VariableBinding> = oids.map { VariableBinding(it) },
    reqId: Int? = null,
    ei: Int = 0, es: Int = 0,
) = PDU(type, vbl).apply {
    reqId?.let { reqId -> requestID = Integer32(reqId) }
    errorIndex = ei
    errorStatus = es
}

sealed class Result(val request: Request) {
    class Response(request: Request, val received: SnmpEvent) : Result(request)
    class Timeout(request: Request) : Result(request)
    class Exception(request: Request, val ex: Throwable) : Result(request)

    fun onResponse(op: (Response) -> Any?): Result = also { if (it is Response) op(it) }
    fun onException(op: (Exception) -> Any?): Result = also { if (it is Exception) op(it) }
    fun onTimeout(op: (Timeout) -> Any?): Result = also { if (it is Timeout) op(it) }
}

suspend fun snmpUnicast(
    strAdr: String = "127.0.0.1",
    reqType: Int = PDU.GETNEXT,
    strOid: String = ".1.3.6",
    strOids: List<String> = listOf(strOid),
    oids: Array<OID> = strOids.map { OID(it) }.toTypedArray(),

    port: Int = 161,
    udpAdr: UdpAddress = UdpAddress(InetAddress.getByName(strAdr), port),
    commStrV1: String = "public",
    retries: Int = 5,
    interval: Duration = 5.seconds,
    intervalMs: Long = interval.inWholeMilliseconds,
    target: SnmpTarget = SnmpTarget(udpAdr, OctetString(commStrV1)).apply {
        this.retries = retries
        timeout = intervalMs
    },
    vbl: List<VariableBinding> = oids.map { VariableBinding(it) },
    pdu: PDU = PDU(reqType, vbl),
    req: Request = Request(target, pdu),
    snmp: Snmp = defaultSenderSnmp,
) = suspendCancellableCoroutine { conti ->
    val listener: ResponseListener = object : ResponseListener {
        override fun <A : Address?> onResponse(event: ResponseEvent<A>) {
            (event.source as Snmp).cancel(event.request, this)
            val response = event.response
            if (response == null) {
                conti.resume(Result.Timeout(req))
            } else {
                @Suppress("UNCHECKED_CAST")
                conti.resume(Result.Response(req, event as SnmpEvent))
            }
        }
    }
    snmp.send(req.pdu, req.target, null, listener)
}

fun ByteArray.toIpV4Adr(): InetAddress = InetAddress.getByAddress(this)

@OptIn(ExperimentalUnsignedTypes::class)
fun UByteArray.toIpV4Adr(): InetAddress = InetAddress.getByAddress(toByteArray())

@OptIn(ExperimentalUnsignedTypes::class)
fun UByteArray.toIpV4ULong(): ULong = fold(0UL) { a: ULong, e: UByte -> (a shl 8) + e }

fun UInt.toUByteArray() = UByteArray(4) { i -> ((this shr ((3 - i) * 8)) and 0xffU).toUByte() }
fun ULong.toUByteArray() = UByteArray(4) { i -> ((this shr ((3 - i) * 8)) and 0xffUL).toUByte() }

fun ULong.toIpV4Adr() = toUByteArray().toIpV4Adr()
fun UInt.toIpV4Adr() = toUByteArray().toIpV4Adr()

fun UInt.toIpV4String() = toUByteArray().joinToString(".")
fun ULong.toIpV4String() = toUByteArray().joinToString(".")
fun String.toIpV4Adr(): InetAddress = InetAddress.getByName(this)

//fun String.toIpV4ULong() = trim().ifEmpty { throw Exception("Exception: Empty '$this' ") }.toIpV4Adr().toIpV4ULong()
fun String.toIpV4UInt() = trim().ifEmpty { throw Exception("Exception: Empty '$this' ") }.toIpV4Adr().toIpV4UInt()

@OptIn(ExperimentalUnsignedTypes::class)
fun InetAddress.toIpV4UByteArray() = address.toUByteArray()

//fun InetAddress.toIpV4ULong() = addressss.fold(0UL) { a: ULong, e: Byte -> (a shl 8) + e.toUByte() }
fun InetAddress.toIpV4UInt() = address.fold(0U) { a: UInt, e: Byte -> (a shl 8) + e.toUByte() }

@OptIn(ExperimentalUnsignedTypes::class)
fun InetAddress.toIpV4String() = toIpV4UByteArray().joinToString(".")

class RateLimiter(
    val interval: Duration,
    val unit: Int,
    val origin: Instant = now(),
) {
    private val tokenChannel = Channel<Unit>(Channel.RENDEZVOUS)

    init {
        CoroutineScope(Dispatchers.Default).launch {
            var nextExecutionTime = origin
            while (isActive) {
                val currentTime = now()
                val passedIntervals = ((currentTime - origin) / interval + 1.0).toInt()
                nextExecutionTime = origin + interval * passedIntervals
                if (currentTime < nextExecutionTime) delay(nextExecutionTime - currentTime)
                tokenChannel.send(Unit)
            }
        }
    }

    suspend fun <T> runIntermittent(block: suspend () -> T): T {
        tokenChannel.receive()
        return block()
    }
}

fun AppData.rateLimiter() = scanRate().let { RateLimiter(it[1].milliseconds, unit = it[0]) }

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.throttled(rateLimiter: RateLimiter): Flow<T> = flow {
    chunked(rateLimiter.unit).collect { rateLimiter.runIntermittent { it.forEach { emit(it) } } }
}

@OptIn(ExperimentalTime::class, ExperimentalUnsignedTypes::class, ExperimentalCoroutinesApi::class)
fun snmpSendFlow(
    ipRange: IpV4RangeSet,
    snmp: Snmp = defaultSenderSnmp,
    rateLimiter: RateLimiter,
    scrambleBlock: Int,
    requestBuilder: (ip: UInt) -> Request = { ip -> Request(ip.toIpV4String(), reqId = ip) },
): Flow<Result> = callbackFlow {
    var count = 0
    val total = ipRange.totalLength().toInt()
    val listener = object : ResponseListener {
        override fun <A : Address?> onResponse(r: ResponseEvent<A>) {
            runCatching {
                val reqAdr = r.response?.requestID?.value?.toUInt()
                val resAdr = r.peerAddress?.toByteArray()?.toUByteArray()?.toIpV4ULong()?.toUInt()

                @Suppress("UNCHECKED_CAST")
                val res = when {
                    resAdr != null && resAdr == reqAdr -> Result.Response(r.userObject as Request, r as SnmpEvent)
                    else -> Result.Timeout(r.userObject as Request)
                }
                trySendBlocking(res) // 受信スレッドをブロックする、send制限が必要
                snmp.cancel(r.request, this)
            }.onFailure { /* Ignored */ }
            if (++count >= total) close()
        }
    }
    ipRange.asUIntFlatSequence().asFlow().scrambled(scrambleBlock).throttled(rateLimiter)
        .collect { ip ->
            val req = requestBuilder(ip)
            runCatching {
                snmp.send(req.pdu, req.target, req, listener)
            }.onFailure { println("Error: $it") }//todo
            if (++count >= total) close()
        }
    awaitClose {}
}.cancellable()

typealias Ipv4Int = UInt

@OptIn(ExperimentalUnsignedTypes::class)
fun Flow<Request>.send(snmp: Snmp = defaultSenderSnmp): Flow<Result> = callbackFlow {
    var cSend = 0
    var cRes = 0
    var sendCmpl = false
    val listener = object : ResponseListener {
        override fun <A : Address?> onResponse(r: ResponseEvent<A>) {
            ++cRes
            val res = runCatching {
                val reqAdr = (r.userObject as Request).target.address.inetAddress.toIpV4UInt()
                val resAdr = r.peerAddress?.toByteArray()?.toUByteArray()?.toIpV4ULong()?.toUInt()
                @Suppress("UNCHECKED_CAST")
                when {
                    resAdr != null && resAdr == reqAdr -> Result.Response(r.userObject as Request, r as SnmpEvent)
                    else -> Result.Timeout(r.userObject as Request)
                }
            }.getOrElse { Result.Exception(r.userObject as Request, it) }
            trySendBlocking(res)
            if (sendCmpl && cRes >= cSend) this@callbackFlow.close()
        }
    }
    val sendJob = launch {
        onCompletion { sendCmpl = true }.collect { req ->
            ++cSend
            snmp.send(req.pdu, req.target, req, listener)
        }
    }
    awaitClose {
        sendJob.cancel()
    }
}.cancellable()

