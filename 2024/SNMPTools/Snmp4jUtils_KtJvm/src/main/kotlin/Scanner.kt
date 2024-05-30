package jp.wjg.shokkaa.snmp4jutils

import jp.wjg.shokkaa.snmp4jutils.async.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.Instant
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.smi.*
import java.net.InetAddress
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalCoroutinesApi
fun main(args: Array<String>): Unit = runBlocking {
    val ipRangeSet = ULongRangeSet(args.map { arg -> arg.toRange() })
    println(ipRangeSet.joinToString(",") { "${it.start.toIpV4Adr()}..${it.endInclusive.toIpV4Adr()}" })
    val snmp = createDefaultSenderSnmpAsync()
    measureTime {
        scanFlow(ipRangeSet, snmp, maxSessions = 5000).filterResponse().collect {
            println("${it.received.peerAddress} - ${it.received.response.variableBindings[0]}")
        }
    }.also { dt -> println("${ipRangeSet.totalLength()}/[$dt] (${ipRangeSet.totalLength() / dt.inWholeSeconds.toULong()}/s)") }
}

suspend fun <T1, T2> Flow<T1>.measureThroughput(
    last: ULong, ext: () -> String = { "" }, op: suspend Flow<T1>.() -> Flow<T2>
): Flow<T2> {
    val t0 = now()
    var nIn = 0UL
    var nOut = 0UL
    var nOutLast = nOut
    CoroutineScope(Dispatchers.Default).launch {
        while (isActive) {
            val n = now()
            val td = n - t0
            val pc = if (last == 0UL) "-" else "${nOut * 100UL / last}"
            val tp =
                if (td == 0.milliseconds) "-" else "${(nOut - nOutLast) * 1000UL / td.inWholeMilliseconds.toULong()}"
            println("$td, $nOut/$last($pc%), $nIn-$nOut:${nIn - nOut} run, $tp/s ${ext()}")
//            t0 = n
            nOutLast = nOut
            delay(5.seconds)
        }
    }
    return onEach { ++nIn }.op().onEach { ++nOut }
}

// IPv4アドレスについて 0~2^(bitWidth-1)までの連続したアドレスの上位下位ビットを入れ替えたものを生成する
@Suppress("unused")
fun ipV4AddressSequence(netAdr: InetAddress, bitWidth: Int, startIndex: ULong = 0UL) =
    (startIndex until (1UL shl bitWidth)).asSequence()
        .map { it to ((netAdr.toIpV4ULong() and ((-1L).toULong() shl bitWidth)) or it) }
        .map { (i, a) -> i to a.toIpV4Adr() }

fun scrambledIpV4AddressSequence(
    netAdr: InetAddress, bitWidth: Int,
    @Suppress("UNUSED_PARAMETER") startIndex: Long = 0,
) = (0UL until (1UL shl bitWidth)).asSequence()
    .map { it to ((netAdr.toIpV4ULong() and ((-1L).toULong() shl bitWidth)) or it.reverseBit32(bitWidth)) }
    .map { (i, a) -> i to a.toIpV4Adr() }

@Suppress("unused")
fun ipV4AddressRangeSequence(start: InetAddress, end: InetAddress) =
    if (start.toIpV4ULong() <= end.toIpV4ULong()) (start.toIpV4ULong()..end.toIpV4ULong()).asSequence()
        .map { it.toIpV4Adr() }
    else sequenceOf()

private fun ULong.reverseBit32(width: Int = 32): ULong {
    var x = this
    x = ((x and 0x55555555UL) shl 1) or ((x and 0xAAAAAAAAUL) shr 1)
    x = ((x and 0x33333333UL) shl 2) or ((x and 0xCCCCCCCCUL) shr 2)
    x = ((x and 0x0F0F0F0FUL) shl 4) or ((x and 0xF0F0F0F0UL) shr 4)
    x = ((x and 0x00FF00FFUL) shl 8) or ((x and 0xFF00FF00UL) shr 8)
    return ((x shl 16) or (x shr 16)) shr (32 - width)
}

@Suppress("unused")
@ExperimentalTime
suspend fun delayUntilNextPeriod(
    interval: Duration,
    now: Instant = now(),
    start: Instant = Instant.fromEpochMilliseconds(0),
): Instant {
    val runTime = when {
        now < start -> start
        else -> start + (interval.inWholeMilliseconds * ((now - start + interval) / interval).roundToLong()).milliseconds
    }
    delay(runTime - now)
    return runTime
}

class RequestBuilder(
    val ip: InetAddress, var target: SnmpTarget? = null, var pdu: PDU? = null, val userData: Any? = null
) {
    fun defaultSnmpFlowTarget(ip: InetAddress) = CommunityTarget(UdpAddress(ip, 161), OctetString("public"))
    fun defaultSnmpScanPdu() = PDU(PDU.GETNEXT, listOf(VariableBinding(OID("1.3.6"))))
    fun build() = Request(target!!, pdu ?: defaultSnmpScanPdu(), userData)
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun scanFlow(
    ipRangeSet: ULongRangeSet,
    snmpAsync: SnmpAsync = createDefaultSenderSnmpAsync(),
    maxSessions: Int = 1000,
    requestBuilder: RequestBuilder.() -> Unit = {
        target = defaultSnmpFlowTarget(ip)
        pdu = defaultSnmpScanPdu()
    }
): Flow<Result> = ipRangeSet.toFlatFlow().map { nip ->
    RequestBuilder(nip.toIpV4Adr()).apply { requestBuilder() }.build()
}.uniCast(snmpAsync, maxSessions)

suspend fun Flow<Request>.uniCast(
    snmpAsync: SnmpAsync = createDefaultSenderSnmpAsync(),
    maxSessions: Int = 1000,
): Flow<Result> = callbackFlow {
    var cmpl = false
    var nReq = 0UL
    var nRes = 0UL
    val sem = Semaphore(maxSessions)
    val responseHandler = object : ResponseListener {
        override fun <A : Address> onResponse(r: ResponseEvent<A>) {
            ++nRes
            @Suppress("UNCHECKED_CAST") val res = when {
                r.response == null -> Timeout(r.userObject as Request)
                else -> Response(r.userObject as Request, r as SnmpEvent)
            }
            trySendBlocking(res)
            sem.release()
//            snmpAsync.snmp.cancel(r.request,this)
            if ((nRes >= nReq) && cmpl) close()
        }
    }
    onEach { req ->
        yield()
        sem.acquire()
        snmpAsync.snmp.send(req.pdu, req.target, req, responseHandler)
        ++nReq
    }.onCompletion { cmpl = true }.collect {}
    awaitClose {}
}

 fun Flow<Request>.send(
    snmpAsync: SnmpAsync = createDefaultSenderSnmpAsync(),
    maxSessions: Int = 1_000,
): Flow<Result> = callbackFlow {
    val snmp = snmpAsync.snmp

    val sem = Semaphore(maxSessions)
    val listener = object : ResponseListener {
        override fun <A : Address?> onResponse(r: ResponseEvent<A>) {
            runCatching {
                @Suppress("UNCHECKED_CAST") val res = when {
                    r.response == null -> Timeout(r.userObject as Request)
                    else -> Response(r.userObject as Request, r as SnmpEvent)
                }
                trySendBlocking(res)
                snmp.cancel(r.request, this)
            }.onFailure { it.printStackTrace() }
            sem.release()
        }
    }
    collect { req ->
        val req0=req.copy()
        print("\r${sem.availablePermits}   ")
        sem.acquire()
        snmp.send(req0.pdu, req0.target, req0, listener)
    }
    awaitClose {}
}

fun ClosedRange<ULong>.asFlow() = flow { for (i in start..endInclusive) emit(i) }

@ExperimentalCoroutinesApi
fun MutableRangeSet<ULong>.toFlatFlow() = asFlow().flatMapConcat { it.asFlow() }
fun Flow<Result>.filterResponse() =
    filter { it is Response }.map { it as Response }.filter { it.request.target.address == it.received.peerAddress }


fun String.toRange() = trim().split("-").map { it.trim().toIpV4ULong() }.let { it[0]..it[it.lastIndex] }
fun String.toRangeSet() =
    split(Regex("[,\\s]")).mapNotNull { runCatching { it.toRange() }.getOrNull() }.let { ULongRangeSet(it) }

