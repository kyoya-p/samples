package jp.`live-on`.shokkaa

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.snmp4j.asn1.BER
import org.snmp4j.smi.*


// VariableBindingを1つの文字列にエンコード/デコード
// OID ' ' Syntax ' ' Value
// Valueに関して、0~0x1f,0x80~0xff,':', 以外は':xx'にエスケープ
// TODO 手抜き:本来はJsonObjectに変換すべき

@ExperimentalSerializationApi
val serializersModule = SerializersModule {
    contextual(VariableAsStringSerializer)
    contextual(VariableBindingAsStringSerializer)
}

@ExperimentalSerializationApi
val jsonSnmp4j = Json {
    prettyPrint = true
    this.serializersModule = serializersModule
}

@ExperimentalSerializationApi
@Serializer(forClass = VariableBinding::class)
object VariableAsStringSerializer : KSerializer<Variable> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("VariableBinding", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Variable) {
        val sValue = when (value) {
            is OctetString -> value.value.escaped()
            else -> value.toString()
        }
        encoder.encodeString("%s %s".format(value.syntax, sValue))
    }

    override fun deserialize(decoder: Decoder): Variable {
        val (sStx, sValue) = decoder.decodeString().split(" ", limit = 2)
        val stx = sStx.toByte()
        val value = when (stx) {
            BER.INTEGER32, BER.COUNTER32 -> Integer32(sValue.toInt())
            BER.OCTETSTRING -> OctetString(sValue.unescaped())
            BER.OID -> org.snmp4j.smi.OID(sValue)
            BER.NULL -> Null()
            // TODO 中略...
            else -> throw Exception("Illegal Syntax :${stx}")
        }
        return value
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
            BER.OID -> org.snmp4j.smi.OID(sValue)
            BER.NULL -> Null()
            // TODO 中略...
            else -> throw Exception("Illegal Syntax :${stx}")
        }
        return VariableBinding(org.snmp4j.smi.OID(sOid), value)
    }

}

private val digit = "0123456789abcdef".map { it.code.toByte() }
private val ascii = List(256) {
    when {
        ('0'..'9').contains(it.toChar()) -> it - '0'.code
        ('a'..'f').contains(it.toChar()) -> it - 'a'.code + 10
        ('A'..'F').contains(it.toChar()) -> it - 'A'.code + 10
        else -> -1
    }
}

fun ByteArray.escaped() = fold(ByteArray(0)) { a, b ->
    val d = b.toUByte().toInt()
    a + when {
        d < 0x20 || d >= 0x7f || d == 0x3a -> arrayOf(0x3a, digit[d / 0x10], digit[d and 0xf])
        else -> arrayOf(d)
    }.map { it.toByte() }.toByteArray()
}.decodeToString()

fun String.unescaped() = encodeToByteArray().let { ba ->
    generateSequence(0 to 0.toByte()) { (i, _) ->
        when {
            i >= ba.size -> null
            ba[i] == ':'.code.toByte() -> {
                if (i >= ba.size - 2) throw Exception("Illegal Escape Position: Index=$i: \"${ba.joinToString()}\" ")
                val c1 = ascii[ba[i + 1].toUByte().toInt()]
                val c2 = ascii[ba[i + 2].toUByte().toInt()]
                if (c1 < 0 || c2 < 0) throw Exception("Illegal Escape Code: Index=$i: \"${ba.joinToString()}\" ")
                (i + 3) to (c1 * 0x10 + c2).toByte()
            }
            else -> (i + 1) to ba[i]
        }
    }.drop(1).map { it.second }.toList().toByteArray()
}
