package mibtool

import com.charleskorn.kaml.Yaml
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.asn1.BER.ENDOFMIBVIEW
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.net.InetAddress

fun main(args: Array<String>) {
    if (args.size == 0) {
        println("syntax: java -jar walk.jar params...")
        println("example: java -jar walk.jar addr: 192.168.1.2, oids: [1.3.6.1,1.3.6.2] ")
        println("params=")
        println(Yaml.default.stringify(SnmpParams.serializer(), SnmpParams("<addr>")))
        System.exit(-1)
    }
    val p = Yaml.default.parse(SnmpParams.serializer(), "{" + args.joinToString(" ") + "}")

    val transport = DefaultUdpTransportMapping()
    val snmp = Snmp(transport)
    transport.listen()

    val target = CommunityTarget(UdpAddress(InetAddress.getByName(p.addr), 161), OctetString(p.comm))
    target.version = SnmpConstants.version2c
    target.timeout = 5_000 //ms

    val initVbl = listOf(VariableBinding(OID(p.oid)))
    snmp.walk(initVbl, target).map { it[0] }.map { VB(it.oid.toString(), it.variable) }.forEach {
        println(Yaml.default.stringify(VB.serializer(), it))
    }
}

fun <A : Address> Snmp.walk(initVbl: List<VariableBinding>, target: CommunityTarget<A>) = generateSequence(initVbl) { vbl ->
    send(PDU(PDU.GETNEXT, vbl), target).response.variableBindings
}.drop(1).takeWhile { it.zip(initVbl).any { (vb, ivb) -> vb.syntax != ENDOFMIBVIEW && vb.oid.startsWith(ivb.oid) } }

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
