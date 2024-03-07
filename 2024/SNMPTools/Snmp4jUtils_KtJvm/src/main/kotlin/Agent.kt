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

suspend fun snmpReceiverFlow_x(snmp: SnmpAsync) = callbackFlow {
    val commandResponder = object : CommandResponder {
        override fun <A : Address?> processPdu(event: CommandResponderEvent<A>?) {
            @Suppress("UNCHECKED_CAST")
            trySend(event as CommandResponderEvent<UdpAddress>)
        }
    }
    snmp.snmp.addCommandResponder(commandResponder)
    awaitClose { snmp.snmp.removeCommandResponder(commandResponder) }
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


@OptIn(ExperimentalCoroutinesApi::class)
suspend fun snmpAgent_x(
    vbl: List<VariableBinding>,
    port: Int = 161,
    snmp: Snmp = Snmp(DefaultUdpTransportMapping(UdpAddress(InetAddress.getByName("0.0.0.0"), port))),
//    hook: (ResponderEvent, PDU) -> PDU = { _, pdu -> pdu },
    onReceiveHook: (ResponderEvent) -> Flow<ResponderEvent> = { flowOf(it) }
) {
    val snmpAsync = snmp.async().apply { listen() }
    val oidVBMap = TreeMap<OID, VariableBinding>().apply {
        vbl.forEach { put(it.oid, VariableBinding(it.oid, it.variable)) }
    }
    snmpReceiverFlow_x(snmpAsync).flatMapConcat { onReceiveHook(it) }.map { event ->
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
        Request(resTarget, resPdu)
    }.collect { res -> snmpAsync.uniCast(res) }
}




