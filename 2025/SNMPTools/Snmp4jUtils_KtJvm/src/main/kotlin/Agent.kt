package jp.wjg.shokkaa.snmp4jutils.async

import jp.wjg.shokkaa.snmp4jutils.decodeFromStream
import jp.wjg.shokkaa.snmp4jutils.yamlSnmp4j
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
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

suspend fun snmpReceiverFlow(snmp: Snmp): Flow<ResponderEvent> {
    fun commandResponder(responseHandler: (ResponderEvent) -> Unit) = object : CommandResponder {
        override fun <A : Address?> processPdu(event: CommandResponderEvent<A>?) {
            @Suppress("UNCHECKED_CAST")
            responseHandler(event as CommandResponderEvent<UdpAddress>)
        }
    }
    return callbackFlow {
        snmp.addCommandResponder(commandResponder { trySend(it) })
        awaitClose { snmp.close() }
    }
}


@OptIn(DelicateCoroutinesApi::class)
@Suppress("BlockingMethodInNonBlockingContext", "BlockingMethodInNonBlockingContext")
suspend fun snmpAgent(
    vbl: List<VariableBinding>,
    snmp: Snmp = Snmp(DefaultUdpTransportMapping(UdpAddress(InetAddress.getByName("0.0.0.0"), 161))).apply { listen() },
    hook: (ResponderEvent, PDU) -> PDU = { _, pdu -> pdu },
) {
    val senderSnmp = createDefaultSenderSnmpAsync()
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
                    else -> oidVBMap[vb.oid]
                } ?: VariableBinding(vb.oid, noSuchObject).also {
                    errorStatus = PDU.noSuchName
                    if (errorIndex == 0) errorIndex = i
                }
            }
        }

        val resTarget = CommunityTarget(event.peerAddress, OctetString("public"))
        GlobalScope.launch { senderSnmp.sendAsync(hook(event, resPdu), resTarget) }
    }
}


suspend fun snmpReceiverFlow_x(snmp: Snmp, op: suspend (Snmp) -> Unit = {}) = callbackFlow {
    val commandResponder = object : CommandResponder {
        override fun <A : Address?> processPdu(event: CommandResponderEvent<A>?) {
            @Suppress("UNCHECKED_CAST")
            trySend(event as CommandResponderEvent<UdpAddress>)
        }
    }
    snmp.addCommandResponder(commandResponder)
    awaitClose { snmp.removeCommandResponder(commandResponder) }
}

suspend fun snmpAgent_x(
    vbl: List<VariableBinding>,
    oidVbMap: LinkedHashMap<OID, VariableBinding> = linkedMapOf(* vbl.sortedBy { it.oid }.map { it.oid to it }
        .toTypedArray()),
    port: Int = 161,
    adr: UdpAddress = UdpAddress(InetAddress.getByName("0.0.0.0"), port),
    snmp: Snmp = Snmp(DefaultUdpTransportMapping(adr)).apply { listen() },
    op: suspend (Snmp) -> Unit = {}
) = snmpReceiverFlow_x(snmp, op).collect { event ->
    fun <K : Comparable<K>, V> Map<K, V>.higher(k: K) = keys.find { it > k }?.let { get(it) }
    val resPdu = PDU().apply {
        type = PDU.RESPONSE
        requestID = event.pdu.requestID
        errorIndex = 0
        errorStatus = PDU.noError
        variableBindings = event.pdu.variableBindings.mapIndexed { i, vb ->
            when (event.pdu.type) {
                PDU.GETNEXT -> oidVbMap.higher(vb.oid)
                else -> oidVbMap[vb.oid]
            } ?: VariableBinding(vb.oid, noSuchObject).also {
                errorStatus = PDU.noSuchName
                if (errorIndex == 0) errorIndex = i
            }
        }
    }
    val resTarget = CommunityTarget(event.peerAddress, OctetString("public"))
    snmp.send(resPdu, resTarget)
}





