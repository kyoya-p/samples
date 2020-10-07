package mibtool

import kotlinx.serialization.*
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.UnionKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
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
        val stx = vb.jsonObject["stx"]?.primitive?.int!!
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

    @ImplicitReflectionSerializer
    override fun serialize(encoder: Encoder, v: Variable) {
        val o = encoder as JsonOutput
        o.encodeJson(JsonObject(mapOf("stx" to JsonPrimitive(v.syntax))))
        o.encodeJson(JsonObject(mapOf("v" to JsonPrimitive(v.toString()))))
    }
}

