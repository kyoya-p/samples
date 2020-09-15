package  mibtool

import org.snmp4j.*
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.io.File
import java.util.*
import java.util.concurrent.Semaphore

typealias MibMap = TreeMap<OID, VariableBinding>

fun MibMap.getNext(oid: OID) = higherEntry(oid)?.value

fun main(args: Array<String>) {
    val mibMap = File(args[0]).run {
        val lines = readLines()
        val nMib = lines[0].toInt()
        lines.drop(1).asSequence().take(nMib).map { it.toVariableBinding() }
    }.run { MibMap().also { m -> forEach { m[it.oid!!] = it } } }
    mibMap.map { (a, b) -> b }.forEachIndexed { i, v ->
        println("$i $v")
    }
    val tm = DefaultUdpTransportMapping(UdpAddress("10.36.102.84".toInetAddr(), 161))
    Snmp(tm).use { snmp ->
        snmp.addCommandResponder(
                object : CommandResponder {
                    override fun <A : Address> processPdu(ev: CommandResponderEvent<A>) {
                        val resVBL = ev.pdu.variableBindings.map { vb ->
                            when (ev.pdu.type) {
                                PDU.GETNEXT -> mibMap.getNext(vb.oid)
                                else -> mibMap.get(vb.oid)
                            } ?: VariableBinding(vb.oid, Null.endOfMibView)
                        }
                        val resPdu = PDU(ev.pdu).apply {
                            type = PDU.RESPONSE
                            variableBindings = resVBL
                            errorStatus = if (resVBL.all { it.variable != Null.endOfMibView }) 0 else PDU.noSuchName
                            errorIndex = if (errorStatus == 0) 0 else resVBL.map { it.variable }.indexOf(Null.endOfMibView)
                        }
                        val target = CommunityTarget<A>().apply {
                            community = OctetString(ev.securityName)
                            address = ev.peerAddress
                            version = SnmpConstants.version1
                            timeout = 0
                            retries = 0
                        }
                        println("Req:${ev.peerAddress} PDU:{ty:${PDU.getTypeString(ev.pdu.type)}, vb:${ev.pdu.variableBindings} "
                                + "=> ResPDU:{ty:${PDU.getTypeString(resPdu.type)} er:${resPdu.errorStatus} ei:${resPdu.errorIndex} vb:${resPdu.variableBindings}"
                        )
                        snmp.send(resPdu, target)
                    }
                }
        )
        snmp.listen()
        println("started.")
        Semaphore(0).acquire()
    }
    println("term.")
}

