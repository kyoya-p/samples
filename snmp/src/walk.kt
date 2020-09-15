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
    val param = Yaml.default.parse(SnmpParams.serializer(), "{" + args.joinToString(" ") + "}")

    val transport = DefaultUdpTransportMapping()
    Snmp(transport).use { snmp ->
        transport.listen()

        val target = CommunityTarget(UdpAddress(InetAddress.getByName(param.addr), 161), OctetString(param.comm))
        target.version = SnmpConstants.version2c
        target.timeout = 5_000 //ms

        val initVbl = listOf(VariableBinding(OID(param.oid)))
        snmp.walk(initVbl, target).map { it[0] }.forEach {
            val v = it.variable.toVariableString()
            println("${it.oid} ${it.syntax} ${v}")
        }
    }
}

fun <A : Address> Snmp.walk(initVbl: List<VariableBinding>, target: CommunityTarget<A>) = generateSequence(initVbl) { vbl ->
    send(PDU(PDU.GETNEXT, vbl), target).response?.variableBindings
}.drop(1).takeWhile { it.zip(initVbl).any { (vb, ivb) -> vb.syntax != ENDOFMIBVIEW && vb.oid.startsWith(ivb.oid) } }
