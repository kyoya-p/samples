import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.snmp4j.asn1.BER
import org.snmp4j.smi.*

// VariableBindingを1つの文字列にエンコード/デコード(手抜き)
// OID ' ' Syntax ' ' Value(0~0x1f,0x80~0xff,':', 以外は':xx'にエスケープ)
object VariableBindingAsStringSerializer : KSerializer<VariableBinding> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("VariableBinding", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: VariableBinding) =
        encoder.encodeString("%s %s %s".format(value.oid.toDottedString(), value.syntax, value.variable.toString()))

    override fun deserialize(decoder: Decoder): VariableBinding {
        val (sOid, sStx, sValue) = decoder.decodeString().split(" ", limit = 3)
        val stx = sStx.toByte()
        val value = when (stx) {
            BER.INTEGER32 -> Integer32(sValue.toInt())
            BER.OCTETSTRING -> OctetString(sValue)
            BER.OID -> OID(sValue)
            BER.NULL -> Null()
            else -> throw Exception("Illegal Syntax :${stx}")
        }
        return VariableBinding(OID(sOid), value)
    }
}


