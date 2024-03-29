package jp.wjg.shokkaa.snmp4jutils

import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlinx.datetime.todayAt
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.snmp4j.PDU
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.*
import java.io.File
import java.math.BigInteger
import java.net.InetAddress
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@ExperimentalSerializationApi
@ExperimentalCoroutinesApi
@FlowPreview
@ExperimentalTime
fun main(args: Array<String>) = runBlocking {
    val baseHost = args.getOrNull(0) ?: "192.168.3.0"
    val scanBits = args.getOrNull(1)?.toInt() ?: 8

    @Suppress("BlockingMethodInNonBlockingContext")
    val baseIp = InetAddress.getByName(baseHost).toIPv4Long() and (-1L shl scanBits)
    val sendInterval = (args.getOrNull(2)?.toInt() ?: 50).milliseconds
    val baseAdr = baseIp.toIpv4Adr()

    val today = Clock.System.todayAt(currentSystemDefault())
    val resultFile = File("samples/$today-${baseAdr.hostAddress}-$scanBits.yaml")

    val snmpBuilder = SnmpBuilder()

    val snmp = snmpBuilder.udp().v1().build().suspendable()
    snmp.listen()

    val start by lazy { Clock.System.now() }
    fun now() = (Clock.System.now() - start).inWholeMilliseconds
    val sampleVBs = SampleOID.values().map { VariableBinding(OID(it.oid)) }
    //val sampleVBs = listOf<VariableBinding>()
    //val sampleVBs = listOf(SampleOID.sysDescr).map { VariableBinding(it.oid) }

    val detectedIps = mutableSetOf<String>()
    var c = 0
    scrambledIpV4AddressSequence(baseAdr, scanBits).asFlow().map { (i, ip) ->
        async {
            val udpAdr = UdpAddress(ip, 161)
            val target = snmpBuilder.target(udpAdr).community(OctetString("public"))
                .timeout(5000).retries(2)
                .build()
            print("\r${i + 1}/${1 shl scanBits} ${(i + 1) * 1000 / (now() + 1)}[req/s] : send() -> $udpAdr [${++c}] ")
            snmp.send(PDU(PDU.GETNEXT, sampleVBs), target, udpAdr)
        }
    }.onEach { delayUntilNextPeriod(sendInterval) }.buffer(UNLIMITED)
        .map { it.await() }
        .onEach { c-- }
        .filter { it.peerAddress != null && it.response != null && (it.userObject as UdpAddress) == it.peerAddress }
        .map { ev ->
            SNMPLog(ev.peerAddress.inetAddress.hostAddress, ev.response.variableBindings)
        }.collect { snmpLog ->
            if (!detectedIps.contains(snmpLog.ip)) {
                detectedIps.add(snmpLog.ip)
                println(snmpLog.ip)
                @Suppress("BlockingMethodInNonBlockingContext")
                resultFile.appendText(Yaml(serializersModule = snmp4jSerializersModule).encodeToString(listOf(snmpLog)))
                @Suppress("BlockingMethodInNonBlockingContext")
                resultFile.appendText("\n")
            }
        }
    snmp.close()
}

@Serializable
data class SNMPLog(val ip: String, val vbs: List<@Contextual VariableBinding>)

// IPv4アドレスについて 0~2^(bitWidth-1)までの連続したアドレスをの上位下位ビットを入れ替えたものを生成する
private fun Long.toIpv4Adr() = InetAddress.getByAddress(BigInteger.valueOf(this).toByteArray())!!
private fun InetAddress.toIPv4Long() = BigInteger(address).toLong()

@Suppress("unused")
fun ipV4AddressSequence(netAdr: InetAddress, bitWidth: Int, startIndex: Long = 0) =
    (startIndex until (1 shl bitWidth)).asSequence()
        .map { it to ((netAdr.toIPv4Long() and (-1L shl bitWidth)) or it) }
        .map { (i, a) -> i to a.toIpv4Adr() }

fun scrambledIpV4AddressSequence(
    netAdr: InetAddress, bitWidth: Int,
    @Suppress("UNUSED_PARAMETER")
    startIndex: Long = 0,
) =
    (0L until (1 shl bitWidth)).asSequence()
        .map { it to ((netAdr.toIPv4Long() and (-1L shl bitWidth)) or it.reverseBit32(bitWidth)) }
        .map { (i, a) -> i to a.toIpv4Adr() }


private fun Long.reverseBit32(width: Int = 32): Long {
    var x = this
    x = ((x and 0x55555555) shl 1) or ((x and 0xAAAAAAAA) ushr 1)
    x = ((x and 0x33333333) shl 2) or ((x and 0xCCCCCCCC) ushr 2)
    x = ((x and 0x0F0F0F0F) shl 4) or ((x and 0xF0F0F0F0) ushr 4)
    x = ((x and 0x00FF00FF) shl 8) or ((x and 0xFF00FF00) ushr 8)
    return ((x shl 16) or (x ushr 16)) ushr (32 - width)
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