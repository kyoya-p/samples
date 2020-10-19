package mibtool.snmp4jWrapper

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

    Snmp(transport).use { snmp ->
        transport.listen()
        val targetAddress: UdpAddress = UdpAddress(InetAddress.getByName(addr), 161)
        val target: CommunityTarget<UdpAddress> = CommunityTarget<UdpAddress>().apply {
            setAddress(targetAddress)
            community = OctetString("public")
            timeout = 2_000 //ms
            retries = 0
            version = SnmpConstants.version2c
        }

        val oid_sysName = ".1.3.6.1.2.1.1.1"
        val oid_prtGeneralSerialNumber = ".1.3.6.1.2.1.43.5.1.1.17"
        val sampleOids = listOf(oid_sysName, oid_prtGeneralSerialNumber)
        val pdu = PDU(PDU.GETNEXT, sampleOids.map { VariableBinding(OID(it)) })

        val detectedDevSet = mutableSetOf<String>()
        val smh = Semaphore(1).apply { acquire() }
        for (i in 0 until 2) {
            snmp.send(pdu, target, null, object : ResponseListener {
                override fun <A : Address> onResponse(event: ResponseEvent<A>) {
                    val pdu = event.response
                    if (pdu == null) { //Tomeout
                        smh.release()
                    } else {
                        val addr = (event.peerAddress as UdpAddress).inetAddress.hostAddress
                        if (!detectedDevSet.contains(addr)) {
                            op(mibtool.Response(
                                    addr = addr,
                                    pdu = mibtool.PDU(
                                            type = pdu.type,
                                            errIdx = pdu.errorIndex,
                                            errSt = pdu.errorStatus,
                                            vbl = pdu.variableBindings.map(VariableBinding::toVB)
                                    ),
                            ))
                            detectedDevSet.add(addr)
                        }
                    }
                }
            })
            smh.acquire()
        }
        op(null) //callback for timeout
    }
}

