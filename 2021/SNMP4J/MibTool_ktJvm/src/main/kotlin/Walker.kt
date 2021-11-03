import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
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
import org.snmp4j.CommunityTarget
import org.snmp4j.Target
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.asn1.BER
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.*
import java.io.File
import java.net.InetAddress
import kotlin.experimental.and


@ExperimentalSerializationApi
@ExperimentalCoroutinesApi
fun main(args: Array<String>) {
    if (args.size == 0) {
        println("Syntax: java -jar walker.jar addr [topOid]")
        return
    }
    val addr = InetAddress.getByName(args[0])!!
    val topOid = OID(args.getOrNull(1) ?: "1")

    val snmp = SnmpBuilder().udp().v1().threads(2).build()!!
    snmp.listen()

    val json = Json {
        prettyPrint = true
        serializersModule = SerializersModule {
            contextual(VariableBindingAsStringSerializer)
        }
    }

    val res = runBlocking {
        runCatching {
            val topPdu = PDU().apply { variableBindings = listOf(VariableBinding(topOid)) }
            val target = CommunityTarget(UdpAddress(addr, 161), OctetString("public"))
            snmp.walkFlow(topPdu, target).toList()
        }.onFailure { it.printStackTrace() }.getOrNull()
    }
    snmp.close()

    //println(json.encodeToString(res))
    File("${addr.hostAddress}.mib").outputStream().use {
        json.encodeToStream(res, it)
    }
}

@ExperimentalCoroutinesApi
fun Snmp.walkFlow(topPdu: PDU, target: Target<UdpAddress>) = flow<VariableBinding> {
    generateSequence(topPdu) { reqPdu ->
        reqPdu.type = PDU.GETNEXT
        this@walkFlow.send(reqPdu, target).response
    }.drop(1).takeWhile { pdu ->
        pdu.errorStatus == PDU.noError && pdu.variableBindings[0].oid.startsWith(topPdu.variableBindings[0].oid)
    }.forEach {
        emit(it.variableBindings[0])
    }
}

// VariableBindingを1つの文字列にエンコード/デコード
// OID ' ' Syntax ' ' Value
// Valueに関して、0~0x1f,0x80~0xff,':', 以外は':xx'にエスケープ
// TODO 手抜き:本来はJsonObjectに変換すべき

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