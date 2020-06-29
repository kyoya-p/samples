package mibtool

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonInput
import kotlinx.serialization.json.int

import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.smi.*

@ImplicitReflectionSerializer
fun main(args: Array<String>) {
    val vbjson = """{"oid":".1.3.6" ,"stx":4,"value":"111"}"""
    val x = Json.parse(VariableBindingSerializer, vbjson)

    Snmp().use { snmp ->
        val target = CommunityTarget(UdpAddress(args[0]), OctetString("public"))
        val initVbl = listOf(VariableBinding(OID(".1")))
        generateSequence(initVbl) { vbl -> snmp.send(PDU(PDU.GETNEXT, vbl), target).response?.variableBindings }
                .drop(1).takeWhile { vbl -> vbl[0].oid.startsWith(initVbl[0].oid) }
                .forEach {

                }

    }
}


@Serializer(forClass = VariableBinding::class)
object VariableBindingSerializer : KSerializer<VariableBinding> {
    override val descriptor: SerialDescriptor
        get() = TODO("Not yet implemented")

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
        TODO("Not yet implemented")
    }

}
