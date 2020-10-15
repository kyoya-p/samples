package mibtool.snmp4jWrapper

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mibtool.Response
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.TransportMapping
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.net.InetAddress
import java.util.concurrent.Semaphore

fun main() {
    runBlocking {
        launch {
            broadcast("255.255.255.255") {
                if (it == null) {
                    println(it)
                }
            }
        }
    }
}

fun broadcast(addr: String, op: (Response?) -> Unit) {
    val transport: TransportMapping<*> = DefaultUdpTransportMapping()

    val sem = Semaphore(1).apply { acquire() }
    Snmp(transport).use { snmp ->
        transport.listen()
        val targetAddress: UdpAddress = UdpAddress(InetAddress.getByName(addr), 161)
        val target: CommunityTarget<UdpAddress> = CommunityTarget<UdpAddress>().apply {
            setAddress(targetAddress)
            community = OctetString("public")
            timeout = 2_000 //ms
            retries = 2
            version = SnmpConstants.version2c
        }

        val oid_sysName = ".1.3.6.1.2.1.1.1"
        val oid_prtGeneralSerialNumber = ".1.3.6.1.2.1.43.5.1.1.17"
        val sampleOids = listOf(oid_sysName, oid_prtGeneralSerialNumber)
        val pdu = PDU(PDU.GETNEXT, sampleOids.map { VariableBinding(OID(it)) })

        snmp.send(pdu, target, null, object : ResponseListener {
            override fun <A : Address> onResponse(event: ResponseEvent<A>) {
                val pdu = event.response
                if (pdu == null) { //Tomeout
                    op(null)
                    sem.release()
                } else {
                    op(mibtool.Response(
                            addr = event.peerAddress.toString(),
                            pdu = mibtool.PDU(
                                    type = pdu.type,
                                    errIdx = pdu.errorIndex,
                                    errSt = pdu.errorStatus,
                                    vbl = pdu.variableBindings.map(VariableBinding::toVB)
                            ),
                    ))
                }
            }
        })
        sem.acquire()
    }
}

