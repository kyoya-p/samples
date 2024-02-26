package jp.wjg.shokkaa.snmp4jutils

import jp.wjg.shokkaa.snmp4jutils.async.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.datetime.Clock
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
import kotlin.time.ExperimentalTime

fun main(args: Array<String>): Unit = runBlocking {
    val ips = args.map { arg ->
        arg.split("-").map { InetAddress.getByName(it).toIpV4ULong() }.let { r -> r[0]..r.getOrElse(1) { r[0] } }
    }
    println(ips)
    val r = ULongRangeSet(ips)
    println(r.toList())
    createDefaultSenderSnmpAsync().use { snmpAsync ->
        with(snmpAsync) {
            scanFlow(r).filterResponse().collect { res ->
                println("${res.request.target} => ${res.received.response[0].variable}")
            }
        }
    }
}


// IPv4アドレスについて 0~2^(bitWidth-1)までの連続したアドレスの上位下位ビットを入れ替えたものを生成する
@Suppress("unused")
fun ipV4AddressSequence(netAdr: InetAddress, bitWidth: Int, startIndex: ULong = 0UL) =
    (startIndex until (1UL shl bitWidth)).asSequence()
        .map { it to ((netAdr.toIpV4ULong() and ((-1L).toULong() shl bitWidth)) or it) }
        .map { (i, a) -> i to a.toIpV4Adr() }

fun scrambledIpV4AddressSequence(
    netAdr: InetAddress, bitWidth: Int,
    @Suppress("UNUSED_PARAMETER")
    startIndex: Long = 0,
) =
    (0UL until (1UL shl bitWidth)).asSequence()
        .map { it to ((netAdr.toIpV4ULong() and ((-1L).toULong() shl bitWidth)) or it.reverseBit32(bitWidth)) }
        .map { (i, a) -> i to a.toIpV4Adr() }

@Suppress("unused")
fun ipV4AddressRangeSequence(start: InetAddress, end: InetAddress) =
    if (start.toIpV4ULong() <= end.toIpV4ULong()) (start.toIpV4ULong()..end.toIpV4ULong()).asSequence()
        .map { it.toIpV4Adr() }
    else sequenceOf()

fun ipV4AddressRangeFlow(start: InetAddress, end: InetAddress) = channelFlow {
    ipV4AddressRangeSequence(start, end).forEach { send(it) }
}


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
    now: Instant = Clock.System.now(),
    start: Instant = Instant.fromEpochMilliseconds(0),
): Instant {
    val runTime = when {
        now < start -> start
        else -> start + (interval.inWholeMilliseconds * ((now - start + interval) / interval).roundToLong()).milliseconds
    }
    delay(runTime - now)
    return runTime
}

class Builder(var target: SnmpTarget? = null, var pdu: PDU? = null, val userData: Any? = null) {
    fun defaultSnmpFlowTarget(ip: InetAddress, port: Int = 161, commStr: String = "public") =
        CommunityTarget(UdpAddress(ip, port), OctetString(commStr))
}

//context(SnmpAsync)
suspend fun SnmpAsync.scanFlow(
    ipRange: MutableRangeSet<ULong>,
    limit: Int = 256,
    tgSetup: Builder.(ip: InetAddress) -> Unit = {}
): Flow<Result> = callbackFlow {
    val sem = Semaphore(limit)
    var n = ipRange.map { it.endInclusive - it.start + 1UL }.sum()
    ipRange.forEach {
        (it.start..it.endInclusive).forEach {
            val ip = it.toIpV4Adr()
            val builder = Builder().apply { tgSetup(ip) }
            val pdu = builder.pdu ?: PDU(PDU.GETNEXT, listOf(VariableBinding(OID(".1"))))
            val target = builder.target ?: builder.defaultSnmpFlowTarget(ip)
            sem.acquire()
            snmp.send(pdu, target, builder.userData, object : ResponseListener {
                override fun <A : Address> onResponse(r: ResponseEvent<A>) {
                    cancel(pdu, this)
                    @Suppress("UNCHECKED_CAST")
                    val res = when {
                        r.response == null -> Timeout(Request(target, pdu))
                        else -> Received(Request(target, pdu), r as SnmpEvent)
                    }
                    trySendBlocking(res)
                    --n
                    sem.release()
                    if (n == 0UL) close()
                }
            })
        }
    }
    awaitClose {}
}

fun Flow<Result>.filterResponse() = filter { it is Received }.map { it as Received }
    .filter { it.request.target.address == it.received.peerAddress }


fun String.toRange() = trim().split("-").map { it.trim().toIpV4ULong() }.let { it[0]..it[it.lastIndex] }
fun String.toRangeSet() =
    split(Regex("[,\\s]")).mapNotNull { runCatching { it.toRange() }.getOrNull() }.let { ULongRangeSet(it) }