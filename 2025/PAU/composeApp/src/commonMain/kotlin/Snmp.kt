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
import java.net.InetAddress
import kotlin.collections.fold
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Clock.System.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

val defaultSenderSnmp by lazy {
    SnmpBuilder().udp(UdpAddress()).v1().v3().build()!!
}

fun createDefaultSenderSnmp() = defaultSenderSnmp

typealias SnmpTarget = CommunityTarget<UdpAddress>
typealias SnmpEvent = ResponseEvent<UdpAddress>

data class Request(val target: SnmpTarget, val pdu: PDU, val userData: Any? = null) {
    companion object
}

fun Request(
    strAdr: String = "127.0.0.1",
    port: Int = 161,
    udpAdr: UdpAddress = UdpAddress(InetAddress.getByName(strAdr), port),

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

    fun onResponse(op: (Response) -> Any?): Result = also { if (it is Response) op(it) }
    fun getResponseOrNull() = if (this is Response) received else null
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
    snmp: Snmp = createDefaultSenderSnmp()
) = suspendCoroutine { conti ->
    val listener: ResponseListener = object : ResponseListener {
        override fun <A : Address?> onResponse(event: ResponseEvent<A>) {
            println("CB:${Thread.currentThread().name}") //TODO

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

/**
 * レートリミッター。
 * 時刻 origin + interval * n に実行するようディレイを入れる
 */
@OptIn(ExperimentalTime::class)
class RateLimiter @OptIn(ExperimentalTime::class) constructor(
    val interval: Duration,
    val origin: Instant = now() - 10.milliseconds,
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

    suspend fun <T> runRateLimited(block: suspend () -> T): T {
        tokenChannel.receive()
        return block()
    }
}

fun <T> Flow<T>.rateLimited(rateLimiter: RateLimiter): Flow<T> = flow {
    collect { v -> rateLimiter.runRateLimited { emit(v) } }
}.cancellable()

@OptIn(
    ExperimentalAtomicApi::class, ExperimentalCoroutinesApi::class, ExperimentalTime::class,
    ExperimentalUnsignedTypes::class
)
fun snmpSendFlow(
    ipRange: ULongRangeSet,
    snmp: Snmp = createDefaultSenderSnmp(),
    rps: Int,
    scrambleBlock: Int,
    requestBuilder: (ip: ULong) -> Request = { ip -> Request(ip.toIpV4String(), reqId = ip.toUInt()) },
): Flow<Result> = callbackFlow {
    fun Flow<ULong>.scrambled(nBits: Int): Flow<ULong> = flow {
        val mask = (1UL shl nBits) - 1UL
        val rndTable = (0UL..mask).shuffled()
        for (w in 0UL..mask) collect { ip -> if (ip and mask == rndTable[w.toInt()]) emit(ip) }
    }

    var count = 0
    val total = ipRange.totalLength().toInt()
    val listener = object : ResponseListener {
        override fun <A : Address?> onResponse(r: ResponseEvent<A>) {
            println("CB:${Thread.currentThread().name} $count") //TODO

            ++count
            runCatching {
                val reqAdr = r.response?.requestID?.value?.toUInt()
                val resAdr = r.peerAddress?.toByteArray()?.toUByteArray()?.toIpV4ULong()?.toUInt()
                @Suppress("UNCHECKED_CAST")
                when {
                    resAdr != null && resAdr == reqAdr
                        -> trySendBlocking(Result.Response(r.userObject as Request, r as SnmpEvent))

                    else -> trySendBlocking(Result.Timeout(r.userObject as Request))
                }
                snmp.cancel(r.request, this)
            }.onFailure { it.printStackTrace() }
            if (count == total) close()
        }
    }
    ipRange.asFlatSequence().asFlow().scrambled(scrambleBlock).chunked(rps).rateLimited(RateLimiter(1.seconds))
        .collect { ips ->
            ips.forEachIndexed { i, ip ->
                val req = requestBuilder(ip)
                snmp.send(req.pdu, req.target, req, listener)
                if (ip.toIpV4String() == "192.168.11.41") println(ip.toIpV4String())//TODO
            }
        }
    awaitClose {}
}.cancellable()

suspend fun snmpSend(req: Request, snmp: Snmp = createDefaultSenderSnmp()) = suspendCancellableCoroutine { conti ->
    val listener = object : ResponseListener {
        override fun <A : Address?> onResponse(r: ResponseEvent<A>) {
            runCatching {
                @Suppress("UNCHECKED_CAST")
                when {
                    r.response == null -> conti.resume(Result.Timeout(r.userObject as Request))
                    else -> conti.resume(Result.Response(r.userObject as Request, r as SnmpEvent))
                }
                snmp.cancel(r.request, this)
            }.onFailure { it.printStackTrace() }
        }
    }
    snmp.send(req.pdu, req.target, req, listener)
}
