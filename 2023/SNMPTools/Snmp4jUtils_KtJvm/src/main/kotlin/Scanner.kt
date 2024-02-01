package jp.wjg.shokkaa.snmp4jutils

import jp.wjg.shokkaa.snmp4jutils.async.SnmpAsync
import jp.wjg.shokkaa.snmp4jutils.async.createDefaultSenderSnmpAsync
import jp.wjg.shokkaa.snmp4jutils.async.getUdpAddress
import jp.wjg.shokkaa.snmp4jutils.async.sendAsync
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.smi.Integer32
import org.snmp4j.smi.OID
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.VariableBinding
import java.net.InetAddress
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime


@ExperimentalSerializationApi
@ExperimentalCoroutinesApi
@ExperimentalTime
fun main(args: Array<String>): Unit = runBlocking {
    data class IpRange(val start: String, val end: String)

    val ips = args.map {
        it.split("-").map { InetAddress.getByName(it).toIPv4ULong() }.let { r -> r[0]..r.getOrElse(1) { r[0] } }
    }
    println(ips)
    createDefaultSenderSnmpAsync().use { snmpAsync ->
        ips.asFlow().flatMapConcat { ips -> snmpAsync.scanFlow(ips) }.collect {
            println("${it.peerAddress}: ${it.response[0]}")
        }
    }
}


fun ULong.toIpv4Adr() =
    InetAddress.getByAddress(ByteArray(4) { i -> ((this shr ((3 - i) * 8)) and 0xffUL).toByte() })!!

fun InetAddress.toIPv4ULong() = address.fold(0UL) { a: ULong, e: Byte -> (a shl 8) + e.toUByte() }

// IPv4アドレスについて 0~2^(bitWidth-1)までの連続したアドレスの上位下位ビットを入れ替えたものを生成する
@Suppress("unused")
fun ipV4AddressSequence(netAdr: InetAddress, bitWidth: Int, startIndex: ULong = 0UL) =
    (startIndex until (1UL shl bitWidth)).asSequence()
        .map { it to ((netAdr.toIPv4ULong() and ((-1L).toULong() shl bitWidth)) or it) }
        .map { (i, a) -> i to a.toIpv4Adr() }

fun scrambledIpV4AddressSequence(
    netAdr: InetAddress, bitWidth: Int,
    @Suppress("UNUSED_PARAMETER")
    startIndex: Long = 0,
) =
    (0UL until (1UL shl bitWidth)).asSequence()
        .map { it to ((netAdr.toIPv4ULong() and ((-1L).toULong() shl bitWidth)) or it.reverseBit32(bitWidth)) }
        .map { (i, a) -> i to a.toIpv4Adr() }

@Suppress("unused")
fun ipV4AddressRangeSequence(start: InetAddress, end: InetAddress) =
    if (start.toIPv4ULong() <= end.toIPv4ULong()) (start.toIPv4ULong()..end.toIPv4ULong()).asSequence()
        .map { it.toIpv4Adr() }
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

suspend fun SnmpAsync.scanFlow(startAdr: InetAddress, endAdr: InetAddress) = channelFlow {
    ipV4AddressRangeFlow(startAdr, endAdr).collect { ip ->
        launch {
            runCatching {
                val target = CommunityTarget(getUdpAddress(ip, 161), OctetString("public")).apply {
                    timeout = 3000
                    retries = 1
                }
                val pdu = PDU(PDU.GETNEXT, listOf(VariableBinding(OID(".1"))))
                pdu.requestID = Integer32(ip.toIPv4ULong().toInt())
                val res = sendAsync(pdu, target)
                if (res.response != null && res.peerAddress != null && res.peerAddress.inetAddress == ip) send(res)
            }.onFailure { it.printStackTrace() }.getOrNull()
        }
    }
}

suspend fun SnmpAsync.scanFlow(r: ClosedRange<ULong>) = scanFlow(r.start.toIpv4Adr(), r.endInclusive.toIpv4Adr())
