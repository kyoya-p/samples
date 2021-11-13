package jp.`live-on`.shokkaa

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.asn1.BER
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.*
import java.io.File
import java.math.BigInteger
import java.net.InetAddress
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import org.snmp4j.Target

@FlowPreview
@ExperimentalSerializationApi
@ExperimentalCoroutinesApi
@ExperimentalTime
suspend fun main(args: Array<String>) = runBlocking {
    val scanMask = args.getOrNull(1)?.toInt() ?: 8
    val baseAdr = InetAddress.getByName(args.getOrNull(0) ?: "192.168.3.0")!!
    val interval = Duration.milliseconds(args.getOrNull(2)?.toInt() ?: 100)

    val snmpBuilder = SnmpBuilder()

    @Suppress("BlockingMethodInNonBlockingContext")
    val snmp = snmpBuilder.udp().v1().build()!!
    runCatching {
        snmp.listen()

        val start = Clock.System.now()
        fun now() = (Clock.System.now() - start).inWholeMilliseconds

        val res = scrambledIpV4AddressFlow(baseAdr, scanMask).flatMapConcat { adr ->
            callbackFlow<ResponseEvent<UdpAddress>> {
                val udpAdr = UdpAddress(adr, 161)
                val targetBuilder = snmpBuilder.target(udpAdr)
                val target = targetBuilder.community(OctetString("public")).timeout(5000).retries(5).build()!!
                val pdu = PDU().apply {
                    requestID = Integer32(adr.toIPv4Long().toInt())
                    type = PDU.GETNEXT
                    variableBindings = TargetOID.values().map { VariableBinding(OID(it.oid)) }
                }
                snmp.getNext(pdu, target) {
                    println(it.response)
                    trySend(it)
                }
            }
        }.map { r: ResponseEvent<UdpAddress> ->
            println("${now()} ${r.peerAddress} ${r.response} ")
            Device(r.peerAddress.inetAddress.hostAddress!!, r.response.variableBindings)
        }.toList()
        println("\n${now()} Complete.")

        println(jsonSnmp4j.encodeToStream(res, File("build/res.json").outputStream()))
        //, File("res.json").outputStream()))

    }.onFailure { it.printStackTrace() }.getOrNull()
    @Suppress("BlockingMethodInNonBlockingContext")
    snmp.close()
}

fun Snmp.getNext(
    pdu: PDU,
    target: Target<UdpAddress>,
    userHandle: Any? = null,
    cb: (ResponseEvent<UdpAddress>) -> Unit,
) {
    @Suppress("BlockingMethodInNonBlockingContext")
    getNext(pdu, target, userHandle, object : ResponseListener {
        override fun <A : Address?> onResponse(r: ResponseEvent<A>?) {
            r?.peerAddress?.let {
                @Suppress("UNCHECKED_CAST")
                cb(r as ResponseEvent<UdpAddress>)
            }
        }
    })
}

// IPv4アドレスについて 0~2^(bitWidth-1)までの連続したアドレスをの上位下位ビットを入れ替えたものを生成する
private fun Long.toIpv4Addr() = InetAddress.getByAddress(BigInteger.valueOf(this).toByteArray())!!
private fun InetAddress.toIPv4Long() = BigInteger(address).toLong()

fun scrambledIpV4AddressFlow(netAdr: InetAddress, bitWidth: Int) =
    (0L until (1 shl bitWidth)).asFlow()
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

 enum class TargetOID(val oid: String, val oidName: String) {
    sysDescr("1.3.6.1.2.1.1.1", "sysDescr"),
    sysName("1.3.6.1.2.1.1.5", "sysName"),
    hrDeviceDescr("1.3.6.1.2.1.25.3.2.1.3", "hrDeviceDescr"),
    prtGeneralPrinterName("1.3.6.1.2.1.43.5.1.1.16", "prtGeneralPrinterName"),
    prtInputVendorName("1.3.6.1.2.1.43.8.2.1.14", "prtInputVendorName"),
    prtOutputVendorName("1.3.6.1.2.1.43.9.2.1.8", "prtOutputVendorName"),
}

