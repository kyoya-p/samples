package jp.`live-on`.shokkaa

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.snmp4j.PDU
import org.snmp4j.asn1.BER
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.*
import java.math.BigInteger
import java.net.InetAddress
import kotlin.experimental.and
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


@ExperimentalSerializationApi
@ExperimentalCoroutinesApi
@ExperimentalTime
suspend fun main(args: Array<String>) {
    fun String.toLong() = BigInteger(InetAddress.getByName(this).address).toLong()
    fun Long.toAddr() = InetAddress.getByAddress(BigInteger.valueOf(this).toByteArray())
    val interval = Duration.milliseconds(args.getOrNull(2)?.toInt() ?: 20)

    val scanMask = args.getOrNull(1)?.toInt() ?: 8
    val baseAdr = (args.getOrNull(0) ?: "192.168.3.0").toLong() and (-1L shl scanMask)

    val snmpBuilder = SnmpBuilder()

    @Suppress("BlockingMethodInNonBlockingContext")
    val snmp = snmpBuilder.udp().v1().threads(1).build()!!
    runCatching {
        snmp.listen()

        val start = Clock.System.now()
        fun now() = (Clock.System.now() - start).inWholeMilliseconds

        val res = callbackFlow {
            (0L until (1 shl scanMask)).forEach { i ->
                launch {
                    val adr = baseAdr or i.reverseBit32(scanMask)
                    val udpAdr = UdpAddress(adr.toAddr(), 161)
                    val targetBuilder = snmpBuilder.target(udpAdr)
                    val target = targetBuilder.community(OctetString("public")).timeout(5000).retries(5).build()!!
                    val pdu = PDU().apply {
                        requestID = Integer32(i.toInt())
                        type = PDU.GETNEXT
                        variableBindings = TargetOID.values().map { VariableBinding(OID(it.oid)) }
                    }
                    print("\r$i ${now()} ${target.address} ")
                    @Suppress("BlockingMethodInNonBlockingContext")
                    snmp.getNext(pdu, target, null, object : ResponseListener {
                        override fun <A : Address?> onResponse(r: ResponseEvent<A>?) {
                            r?.peerAddress?.let {
                                @Suppress("UNCHECKED_CAST")
                                trySend(r as ResponseEvent<UdpAddress>)
                            }
                        }
                    })
                }
                delay(interval)
            }
            close()
        }.map { r: ResponseEvent<UdpAddress> ->
            println("${now()} ${r.peerAddress} ${r.response} ")
            Device(r.peerAddress.inetAddress.hostAddress!!, r.response.variableBindings)
        }.toList()
        println("\n${now()} Complete.")

        println(jsonSnmp4j.encodeToString(res))
        //, File("res.json").outputStream()))

    }.onFailure { it.printStackTrace() }.getOrNull()
    @Suppress("BlockingMethodInNonBlockingContext")
    snmp.close()
}

@Serializable
private data class Device(
    val ip: String,
    val vbl: List<@Contextual VariableBinding>,
)


fun Long.reverseBit32(width: Int = 32): Long {
    var x = this
    x = ((x and 0x55555555) shl 1) or ((x and 0xAAAAAAAA) ushr 1)
    x = ((x and 0x33333333) shl 2) or ((x and 0xCCCCCCCC) ushr 2)
    x = ((x and 0x0F0F0F0F) shl 4) or ((x and 0xF0F0F0F0) ushr 4)
    x = ((x and 0x00FF00FF) shl 8) or ((x and 0xFF00FF00) ushr 8)
    return ((x shl 16) or (x ushr 16)) ushr (32 - width)
}

private enum class TargetOID(val oid: String, val oidName: String) {
    sysDescr("1.3.6.1.2.1.1.1", "sysDescr"),
    sysName("1.3.6.1.2.1.1.5", "sysName"),
    hrDeviceDescr("1.3.6.1.2.1.25.3.2.1.3", "hrDeviceDescr"),
    prtGeneralPrinterName("1.3.6.1.2.1.43.5.1.1.16", "prtGeneralPrinterName"),
    prtInputVendorName("1.3.6.1.2.1.43.8.2.1.14", "prtInputVendorName"),
    prtOutputVendorName("1.3.6.1.2.1.43.9.2.1.8", "prtOutputVendorName"),
}


// VariableBindingを1つの文字列にエンコード/デコード
// OID ' ' Syntax ' ' Value
// Valueに関して、0~0x1f,0x80~0xff,':', 以外は':xx'にエスケープ
// TODO 手抜き:本来はJsonObjectに変換すべき

@ExperimentalSerializationApi
val jsonSnmp4j = Json {
    prettyPrint = true
    serializersModule = SerializersModule {
        contextual(VariableBindingAsStringSerializer)
    }
}

@ExperimentalSerializationApi
@Serializer(forClass = VariableBinding::class)
object VariableBindingAsStringSerializer : KSerializer<VariableBinding> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("VariableBinding", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: VariableBinding) {
        val v = value.variable
        val sValue = when (v) {
            is OctetString -> v.value.escaped()
            else -> v.toString()
        }
        encoder.encodeString("%s %s %s".format(value.oid.toDottedString(), value.syntax, sValue))
    }

    override fun deserialize(decoder: Decoder): VariableBinding {
        val (sOid, sStx, sValue) = decoder.decodeString().split(" ", limit = 3)
        val stx = sStx.toByte()
        val value = when (stx) {
            BER.INTEGER32, BER.COUNTER32 -> Integer32(sValue.toInt())
            BER.OCTETSTRING -> OctetString(sValue.unescaped())
            BER.OID -> OID(sValue)
            BER.NULL -> Null()
            // TODO 中略...
            else -> throw Exception("Illegal Syntax :${stx}")
        }
        return VariableBinding(OID(sOid), value)
    }

    const val z = '0'.code.toByte()
    fun ByteArray.escaped() = fold(ByteArray(0)) { a, b ->
        a + when {
            b < 0x20 || b >= 0x80 || b == 0x3a.toByte() -> arrayOf(0x3b, z + b / 0x10, z + (b and 0xf))
            else -> arrayOf(b.toInt())
        }.map { it.toByte() }.toByteArray()
    }.decodeToString()

    fun String.unescaped(): ByteArray = encodeToByteArray().let { ba ->
        generateSequence(0 to 0.toByte()) { (i, _) ->
            when {
                ba[i] == ':'.code.toByte() -> i + 3 to ((ba[i + 1] - z) / 0x10 + (ba[i + 2] and 0xf)).toByte()
                else -> i + 1 to ba[i]
            }
        }.takeWhile { (i, _) -> i < ba.size }.map { it.second }.toList().toByteArray()
    }
}
