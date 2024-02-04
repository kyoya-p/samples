package jp.wjg.shokkaa.snmp4jutils

import com.charleskorn.kaml.Yaml
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
import kotlinx.serialization.serializer
import org.snmp4j.asn1.BER
import org.snmp4j.smi.*
import org.snmp4j.smi.SMIConstants.*
import java.io.InputStream
import java.net.InetAddress

@ExperimentalSerializationApi
val snmp4jSerializersModule = SerializersModule {
    contextual(VariableAsStringSerializer)
    contextual(VariableBindingAsStringSerializer)
}

@ExperimentalSerializationApi
val jsonSnmp4j = Json {
    prettyPrint = true
    serializersModule = snmp4jSerializersModule
}

@ExperimentalSerializationApi
val yamlSnmp4j = Yaml(serializersModule = snmp4jSerializersModule)

inline fun <reified R> Yaml.decodeFromStream(s: InputStream): R = decodeFromStream(serializersModule.serializer(), s)
//inline fun <reified R> Yaml.decodeFromString(s: String): R = decodeFromString(serializersModule.serializer(), s)

@ExperimentalSerializationApi
@Serializer(forClass = Variable::class)
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
            BER.OID -> OID(sValue)
            BER.NULL -> Null()
            // TODO 中略...
            else -> throw Exception("Illegal Syntax :${stx}")
        }
        return value
    }
}

// VariableBindingを1つの文字列にエンコード/デコード
// OID ' ' Syntax ' ' Value
// Valueに関して、0~0x1f,0x80~0xff,':', は':xx'にエスケープ
// Note: JsonObjectに変換すべきかもしれないが、コンパクトさを重視し１つのStringに

@ExperimentalSerializationApi
@Serializer(forClass = VariableBinding::class)
object VariableBindingAsStringSerializer : KSerializer<VariableBinding> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("VariableBinding", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: VariableBinding) {
        val v = value.variable

        // for type of value.variable.value
        val sValue = when (v) {
            is OctetString /*BitString,Opaque,SecretOctetString,*/ -> v.value.escaped()
            is UnsignedInteger32 /*TimeTicks,Counter32,Gauge32*/ -> v.value.toString()
            is Integer32 -> v.value.toString()
            is Counter64 -> v.value.toString()
            is OID -> v.value.joinToString(separator = ".")
            is IpAddress -> v.inetAddress.hostAddress!!
            is Null -> ""
            else -> v.toString()
        }

        // for SMIConstants
//        val sValue = when (value.syntax) {
//            SMIConstants.SYNTAX_INTEGER -> (v as Integer32).value.toString()
//            SMIConstants.SYNTAX_OCTET_STRING -> (v as OctetString).value.escaped()
//            SMIConstants.SYNTAX_NULL -> (v as Null).toString()
//            SMIConstants.SYNTAX_OBJECT_IDENTIFIER -> (v as OID).toString()
//            SMIConstants.SYNTAX_IPADDRESS -> (v as).toString()
//            SMIConstants.SYNTAX_INTEGER32 -> (v as).toString()
//            SMIConstants.SYNTAX_COUNTER32 -> (v as).toString()
//            SMIConstants.SYNTAX_GAUGE32 -> (v as).toString()
//            SMIConstants.SYNTAX_UNSIGNED_INTEGER32 -> (v as).toString()
//            SMIConstants.SYNTAX_TIMETICKS -> (v as).toString()
//            SMIConstants.SYNTAX_OPAQUE -> (v as).toString()
//            SMIConstants.SYNTAX_COUNTER64 -> (v as).toString()
//            SMIConstants.SYNTAX_BITS -> (v as).toString()
//            SMIConstants.EXCEPTION_NO_SUCH_OBJECT -> (v as).toString()
//            SMIConstants.EXCEPTION_NO_SUCH_INSTANCE -> (v as).toString()
//            SMIConstants.EXCEPTION_END_OF_MIB_VIEW -> (v as).toString()
//            else -> v.toString()
//        }
        encoder.encodeString("%s %s %s".format(value.oid.toDottedString(), value.syntax, sValue))
    }

    override fun deserialize(decoder: Decoder): VariableBinding {
        val (sOid, sStx, sValue) = decoder.decodeString().split(" ", limit = 3)
        val stx = sStx.toInt()

        // for SMIConstants
        val value = when (stx) {
            SYNTAX_OCTET_STRING -> OctetString(sValue.unescaped())
            SYNTAX_OPAQUE -> Opaque(sValue.unescaped())
            SYNTAX_INTEGER -> Integer32(sValue.toInt())
            SYNTAX_COUNTER32 -> Counter32(sValue.toLong())
            SYNTAX_TIMETICKS -> TimeTicks(sValue.toLong())
            SYNTAX_GAUGE32 -> Gauge32(sValue.toLong())
            SYNTAX_COUNTER64 -> Counter64(sValue.toLong())
            SYNTAX_OBJECT_IDENTIFIER -> OID(sValue)
            SYNTAX_IPADDRESS -> IpAddress(InetAddress.getByName(sValue))
            SYNTAX_NULL -> Null()
            // TODO 中略...
            else -> throw Exception("Illegal Syntax :${stx}")
        }
        return VariableBinding(OID(sOid), value)
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
