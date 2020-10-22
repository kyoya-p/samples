package mibtool.snmp4jWrapper

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

suspend fun main() {
    val transport: TransportMapping<*> = DefaultUdpTransportMapping()
    Snmp(transport).use { snmp ->
        transport.listen()
        broadcastFlow(snmp, "255.255.255.255").collect {
            println(it)
        }
    }
}

fun main2() {
    runBlocking {
        launch {
            broadcastCB("255.255.255.255") {
                if (it == null) {
                    println(it)
                }
            }
        }
    }
}

fun broadcastCB(addr: String, op: (mibtool.ResponseEvent?) -> Any?) {
    val transport: TransportMapping<*> = DefaultUdpTransportMapping()

    Snmp(transport).use { snmp ->
        transport.listen()
        val targetAddress: UdpAddress = UdpAddress(InetAddress.getByName(addr), 161)
        val target: CommunityTarget<UdpAddress> = CommunityTarget<UdpAddress>().apply {
            address = targetAddress
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
                            op(mibtool.ResponseEvent(
                                    peerAddr = addr,
                                    pdu = mibtool.PDU(
                                            type = pdu.type,
                                            errIdx = pdu.errorIndex,
                                            errSt = pdu.errorStatus,
                                            vbl = pdu.variableBindings.map(VariableBinding::toVB)
                                    ),
                                    requestTarget = null
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

fun broadcastFlow2(addr: String) = callbackFlow<mibtool.ResponseEvent> {
    broadcastCB(addr) { res: mibtool.ResponseEvent? ->
        if (res == null) channel.close()
        else offer(res)
    }
    awaitClose()
}

fun broadcastFlow(snmp: Snmp, addr: String) = callbackFlow<mibtool.ResponseEvent> {
    val targetAddress: UdpAddress = UdpAddress(InetAddress.getByName(addr), 161)
    val target: CommunityTarget<UdpAddress> = CommunityTarget<UdpAddress>().apply {
        address = targetAddress
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

    for (i in 0 until 5) {
        snmp.send(pdu, target, null, object : ResponseListener {
            override fun <A : Address> onResponse(event: ResponseEvent<A>) {
                val pdu = event.response
                if (pdu == null) { //Timeout
                    //smh.release()
                } else {
                    val addr = (event.peerAddress as UdpAddress).inetAddress.hostAddress
                    if (!detectedDevSet.contains(addr)) {
                        offer(mibtool.ResponseEvent(
                                peerAddr = addr,
                                pdu = mibtool.PDU(
                                        type = pdu.type,
                                        errIdx = pdu.errorIndex,
                                        errSt = pdu.errorStatus,
                                        vbl = pdu.variableBindings.map(VariableBinding::toVB)
                                ),
                                requestTarget = null,
                        ))
                        detectedDevSet.add(addr)
                    }
                }
            }
        })
        delay(2000)
    }
    channel.close()
    awaitClose()
}


