package mibtool.snmp4jWrapper

import org.snmp4j.*
import org.snmp4j.CommunityTarget
import org.snmp4j.smi.*
import org.snmp4j.PDU
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.util.concurrent.Semaphore

fun main(args: Array<String>) {
    val target = CommunityTarget(UdpAddress(args[0].toInetAddr(), 161), OctetString("public"))

    val tm = DefaultUdpTransportMapping(UdpAddress("10.36.102.84".toInetAddr(), 161))
    Snmp(tm).use { snmp ->
        snmp.addCommandResponder(
                object : CommandResponder {
                    override fun <A : Address> processPdu(ev: CommandResponderEvent<A>) {
                        val reqPdu = ev.pdu!!
                        snmp.send(reqPdu, target)?.let { resEv ->
                            val resPdu=resEv.response
                            println("Req:${ev.peerAddress} PDU:{ty:${PDU.getTypeString(ev.pdu.type)}, vb:${ev.pdu.variableBindings} "
                                    + "=> ResPDU:{ty:${PDU.getTypeString(resPdu.type)} er:${resPdu.errorStatus} ei:${resPdu.errorIndex} vb:${resPdu.variableBindings}"
                            )
                            snmp.send(resPdu, target)
                        }
                    }
                }
        )
         snmp.listen()
        println("started.")
        Semaphore(0).acquire()
    }
    println("term.")
}
