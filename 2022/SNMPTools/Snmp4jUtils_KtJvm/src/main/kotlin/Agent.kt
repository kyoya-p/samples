package jp.wjg.shokkaa.snmp4jutils.async

import jp.wjg.shokkaa.snmp4jutils.decodeFromStream
import jp.wjg.shokkaa.snmp4jutils.yamlSnmp4j
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.*
import org.snmp4j.*
import org.snmp4j.smi.*
import org.snmp4j.smi.Null.noSuchObject
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.io.File
import java.net.InetAddress
import java.util.*

@ExperimentalSerializationApi
fun main(args: Array<String>) = runBlocking {
    val vbl: List<VariableBinding> = yamlSnmp4j.decodeFromStream(File(args[0]).inputStream())
    snmpAgent(vbl)
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun snmpReceiverFlow(snmp: Snmp): Flow<ResponderEvent> {
    fun commandResponder(responseHandler: (ResponderEvent) -> Unit) = object : CommandResponder {
        override fun <A : Address?> processPdu(event: CommandResponderEvent<A>?) {
            @Suppress("UNCHECKED_CAST")
            responseHandler(event as CommandResponderEvent<UdpAddress>)
        }
    }
    println("Receiver[1]")
    return callbackFlow {
        snmp.addCommandResponder(commandResponder {
            println("Receiver[x]:received")
            trySend(it)
        })
        println("Receiver[2]")
        awaitClose {
            snmp.close()
            println("Receiver[9]")
        }
        println("Receiver[3] illegal")
    }
}
// coroutineで実行されます
@Suppress("BlockingMethodInNonBlockingContext", "BlockingMethodInNonBlockingContext")
suspend fun snmpAgent(
    vbl: List<VariableBinding>,
    snmp: Snmp = Snmp(DefaultUdpTransportMapping(UdpAddress(InetAddress.getByName("0.0.0.0"), 161))).apply { listen() },
    hook: (ResponderEvent, PDU) -> PDU = { _, pdu -> pdu },
) {
    val oidVBMap = TreeMap<OID, VariableBinding>().apply {
        vbl.forEach { put(it.oid, VariableBinding(it.oid, it.variable)) }
    }
    snmpReceiverFlow(snmp).collect { event ->
        val resPdu = PDU().apply {
            type = PDU.RESPONSE
            requestID = event.pdu.requestID
            errorIndex = 0
            errorStatus = PDU.noError
            variableBindings = event.pdu.variableBindings.mapIndexed { i, vb ->
                when (event.pdu.type) {
                    PDU.GETNEXT -> oidVBMap.higherEntry(vb.oid)?.value
                    else -> oidVBMap.get(vb.oid)
                } ?: VariableBinding(vb.oid, noSuchObject).also {
                    errorStatus = PDU.noSuchName
                    if (errorIndex == 0) errorIndex = i
                }
            }
        }

        val resTarget = CommunityTarget(event.peerAddress, OctetString("public"))
        //println("${event.peerAddress} => ${event.pdu}")
        println("RES: $resPdu")
        snmp.async().sendAsync(hook(event, resPdu), resTarget)
    }
}

@Suppress("unused")
val sampleMibList = mapOf<OID, Variable>(
    OID(1, 3, 6, 1, 2, 1, 1, 1) to OctetString("Dummy SNMP Agent"),
    OID(1, 3, 6, 1, 2, 1, 1, 2) to OID(1, 3, 6, 1, 2, 1, 1, 1, 1, 1),
    OID(1, 3, 6, 1, 2, 1, 1, 3) to TimeTicks(77777777),
    OID(1, 3, 6, 1, 2, 1, 1, 4) to Integer32(65535),
    OID(1, 3, 6, 9, 0) to OctetString("1.3.6.9.0"),
    OID(1, 3, 6, 9, 1, 2, 3) to OctetString("1.3.6.9.1.2.3")
).map { VariableBinding(it.key, it.value) }



