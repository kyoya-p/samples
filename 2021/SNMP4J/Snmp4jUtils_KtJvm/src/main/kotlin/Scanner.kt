package jp.`live-on`.shokkaa

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.snmp4j.PDU
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.OID
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import suspendable
import java.math.BigInteger
import java.net.InetAddress
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@FlowPreview
@ExperimentalTime
suspend fun main(args: Array<String>) = runBlocking {
    @Suppress("BlockingMethodInNonBlockingContext")
    val baseAdr = InetAddress.getByName(args.getOrNull(0) ?: "192.168.3.0")!!
    val scanMask = args.getOrNull(1)?.toInt() ?: 8
    val interval = Duration.milliseconds(args.getOrNull(2)?.toInt() ?: 100)

    val snmpBuilder = SnmpBuilder()

    @Suppress("BlockingMethodInNonBlockingContext")
    val snmp = snmpBuilder.udp().v1().build().suspendable()
    snmp.listen()

    val start by lazy { Clock.System.now() }
    fun now() = (Clock.System.now() - start).inWholeMilliseconds

    val sampleVBs = SampleOID.values().map { VariableBinding(OID(it.oid)) }
    scrambledIpV4AddressSequence(baseAdr, scanMask).asFlow().map { ip ->
        async {
            val udpAdr = UdpAddress(ip, 161)
            print("$udpAdr ")
            val target = snmpBuilder.target(udpAdr).community(OctetString("public"))
                .timeout(1000).retries(1)
                .build()
            print("${now()}: send() -> ")
            snmp.send(PDU(PDU.GETNEXT, sampleVBs), target)
        }
    }.map { it.await() }.collect { ev ->
        println("${now()} ${ev.peerAddress} ${ev.response?.variableBindings}")
        delay(interval)
    }

    snmp.close()
}

// IPv4アドレスについて 0~2^(bitWidth-1)までの連続したアドレスをの上位下位ビットを入れ替えたものを生成する
private fun Long.toIpv4Addr() = InetAddress.getByAddress(BigInteger.valueOf(this).toByteArray())!!
private fun InetAddress.toIPv4Long() = BigInteger(address).toLong()

fun ipV4AddressSequence(netAdr: InetAddress, bitWidth: Int) =
    (0L until (1 shl bitWidth)).asSequence().map { it.toIpv4Addr() }

fun scrambledIpV4AddressSequence(netAdr: InetAddress, bitWidth: Int) =
    (0L until (1 shl bitWidth)).asSequence()
        .map { (netAdr.toIPv4Long() and (-1L shl bitWidth)) or it.reverseBit32(bitWidth) }
        .map { it.toIpv4Addr() }

@Serializable
internal data class Device(
    val ip: String,
    val vbl: List<@Contextual VariableBinding>,
)


private fun Long.reverseBit32(width: Int = 32): Long {
    var x = this
    x = ((x and 0x55555555) shl 1) or ((x and 0xAAAAAAAA) ushr 1)
    x = ((x and 0x33333333) shl 2) or ((x and 0xCCCCCCCC) ushr 2)
    x = ((x and 0x0F0F0F0F) shl 4) or ((x and 0xF0F0F0F0) ushr 4)
    x = ((x and 0x00FF00FF) shl 8) or ((x and 0xFF00FF00) ushr 8)
    return ((x shl 16) or (x ushr 16)) ushr (32 - width)
}

enum class SampleOID(val oid: String, val oidName: String) {
    sysDescr("1.3.6.1.2.1.1.1", "sysDescr"),
    sysName("1.3.6.1.2.1.1.5", "sysName"),
    hrDeviceDescr("1.3.6.1.2.1.25.3.2.1.3", "hrDeviceDescr"),
    prtGeneralPrinterName("1.3.6.1.2.1.43.5.1.1.16", "prtGeneralPrinterName"),
    prtInputVendorName("1.3.6.1.2.1.43.8.2.1.14", "prtInputVendorName"),
    prtOutputVendorName("1.3.6.1.2.1.43.9.2.1.8", "prtOutputVendorName"),
}

