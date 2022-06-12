package mibtool.snmp4jWrapper

import com.charleskorn.kaml.Yaml
import gdvm.device.PDU
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU.GETNEXT
import org.snmp4j.Snmp
import org.snmp4j.asn1.BER.ENDOFMIBVIEW
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.net.InetAddress

@Serializable
data class CommandOption(
    val addr: String,
    val pdu: PDU,
    //val snmpConfig: SnmpConfig = SnmpConfig(),
)


fun main(args: Array<String>) {
    if (args.size == 0) {
        println("syntax: java -jar walk.jar params...")
        println("example: java -jar walk.jar addr: 192.168.1.2, pdu: { vbl: [ { oid: 1.3.6.1 } ] } ")
        println("params=")
        //println(Yaml.default.encodeToString(SnmpConfig.serializer(), SnmpConfig()))
        System.exit(-1)
    }
    val commStr = "{ " + args.joinToString(" ") + " }"
    println(commStr)
    val commandOption = Yaml.default.decodeFromString(CommandOption.serializer(), commStr)
    val vbl = walk(commandOption.pdu, commandOption.addr).toList()
    val format = Json { prettyPrint = true }
    println(format.encodeToString(vbl))
}


fun walk(pdu: PDU, addr: String) = sequence {
    val transport = DefaultUdpTransportMapping()
    Snmp(transport).use { snmp ->
        transport.listen()

        val target = CommunityTarget(
            UdpAddress(InetAddress.getByName(addr), 161),
            OctetString("public"),
        )
        target.version = SnmpConstants.version2c
        target.timeout = 5_000 //ms

//        val initVbl = listOf(VariableBinding(OID(param.oid)))
        val initVbl = pdu.vbl.map { VariableBinding(OID(it.oid)) }
        snmp.walk(org.snmp4j.PDU(GETNEXT, initVbl), target).forEach { yield(it) }
    }
}

fun <A : Address> Snmp.walk(initVbl: List<VariableBinding>, target: CommunityTarget<A>) =
    generateSequence(initVbl) { vbl ->
        send(org.snmp4j.PDU(org.snmp4j.PDU.GETNEXT, vbl), target).response?.variableBindings
    }.drop(1).takeWhile { it.zip(initVbl).any { (vb, ivb) -> vb.syntax != ENDOFMIBVIEW && vb.oid.startsWith(ivb.oid) } }

fun <A : Address> Snmp.walk(initPDU: org.snmp4j.PDU, target: CommunityTarget<A>) = generateSequence(initPDU) { pdu ->
    send(pdu.apply {
        type = org.snmp4j.PDU.GETNEXT; variableBindings = variableBindings.map { VariableBinding(it.oid) }
    }, target).response
}.drop(1).takeWhile {
    it.variableBindings.zip(initPDU.variableBindings)
        .any { (vb, ivb) -> vb.syntax != ENDOFMIBVIEW && vb.oid.startsWith(ivb.oid) }
}
