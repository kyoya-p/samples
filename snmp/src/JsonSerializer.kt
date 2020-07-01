package mibtool

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.snmp4j.smi.*

@Serializer(forClass = VariableBinding::class)
object VariableBindingSerializer : KSerializer<VariableBinding> {
    override val descriptor: SerialDescriptor
        get() = SerialDescriptor("mypackage.VarBind", UnionKind.CONTEXTUAL)

    override fun deserialize(decoder: Decoder): VariableBinding {
        val input = decoder as? JsonInput ?: throw SerializationException("This class can be loaded only by Json")

        val vb = input.decodeJson()
        val oid = vb.jsonObject["oid"].toString()!!
        val stx = vb.jsonObject["stx"]?.int!!
        val value = vb.jsonObject["value"]?.toString()!!

//TODO
//TODO
//TODO
//TODO
//TODO

        println(vb)
        return VariableBinding()
    }

    override fun serialize(encoder: Encoder, value: VariableBinding) {
        val output = encoder as? JsonOutput
                ?: throw SerializationException("This class can be saved only by Json")
        val vb = JsonObject(mapOf("oid" to JsonLiteral(value.oid.toString()),
                "stx" to JsonLiteral(value.syntax),
                "v" to JsonLiteral(value.variable.toString()) //TODO 型チェック
        ))
        output.encodeJson(vb)
    }
}

@Serializer(forClass = Variable::class)
object VariableSerializer : KSerializer<Variable> {
    override val descriptor: SerialDescriptor
        get() = SerialDescriptor("mypackage.Variable", UnionKind.CONTEXTUAL)

    override fun deserialize(decoder: Decoder): Variable {
        TODO("Not yet implemented")
    }

    override fun serialize(encoder: Encoder, v: Variable) {
        val o = encoder as JsonOutput
        o.encodeJson(JsonObject(mapOf("stx" to JsonPrimitive(v.syntax))))
        o.encodeJson(JsonObject(mapOf("v" to JsonPrimitive(v.toString()))))
        o.encodeString("aaa")
        o.encodeStringElement(descriptor,0,"xxx")
    }

}

fun Variable.toString() = when (this) {
    is Integer32 -> value.toString() // 2: Integer32
    is OctetString -> value!!.toOctetString() // 4: OctetString
    is Null -> ByteArray(0).toOctetString() // 5: Null
    is OID -> value.toOidString() // 6: OID
    is IpAddress -> inetAddress.address!!.toOctetString() // 64: IpAddress
    is Counter32 -> value.toString() // 65: Counter32
    is Gauge32 -> value.toString() // 66: Gauge32
    is TimeTicks -> value.toString() // 67: TimeTicks
    is Opaque -> value!!.toOctetString() // 68: Opaque
    is Counter64 -> value.toString() // 70: Counter64
    //128 -> Null(128) // 128: NOSUCHOBJECT (Error)
    //129 -> Null(129) // 129: NOSUCHINSTANCE (Error)
    //130 -> Null(130) // 130: ENDOFMIBVIEW (Error)
    else -> throw IllegalArgumentException("Unsupported variable type: ${javaClass.name}")
}

fun ByteArray.toOctetString() = toUByteArray().map { it.toInt() }.joinToString("", "\"", "\"") {
    if (it <= 0x20 || 0x7f <= it || it == '\"'.toInt() || it == ':'.toInt()) ":%02x".format(it)
    else it.toChar().toString()
}

fun IntArray.toOidString() = joinToString(".", "\"", "\"")

