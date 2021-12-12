package jp.`live-on`.shokkaa

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import org.snmp4j.*
import org.snmp4j.smi.*
import org.snmp4j.smi.Null.noSuchObject
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.net.InetAddress
import java.util.*


typealias ResponderEvent = CommandResponderEvent<UdpAddress>

@ExperimentalCoroutinesApi
suspend fun snmpAgentFlow(snmp: Snmp) = callbackFlow {
    val commandResponder = object : CommandResponder {
        override fun <A : Address?> processPdu(event: CommandResponderEvent<A>?) {
            @Suppress("UNCHECKED_CAST")
            trySend(event as ResponderEvent)
        }
    }
    println("startAgent")
    kotlin.runCatching {
        snmp.addCommandResponder(commandResponder)
        awaitClose {
            snmp.removeCommandResponder(commandResponder)
        }
    }.onFailure { close() }
    println("endAgent")
}

@Suppress("BlockingMethodInNonBlockingContext", "unused")
@ExperimentalCoroutinesApi
suspend fun snmpAgent(
    mibMap: TreeMap<OID, VariableBinding>,
    host: String = "0.0.0.0",
    port: Int = 161,
    op: (ev: ResponderEvent, resPdu: PDU) -> PDU? = { _, pdu -> pdu },
) = Snmp(DefaultUdpTransportMapping(UdpAddress(InetAddress.getByName(host), port))).use { snmp ->
    snmp.listen()
    snmpAgentFlow(snmp).collectLatest { ev ->
        val resPdu0 = PDU().apply {
            type = PDU.RESPONSE
            errorIndex = 0
            errorStatus = PDU.noError
            variableBindings = ev.pdu.variableBindings.mapIndexed { i, vb ->
                when (ev.pdu.type) {
                    PDU.GETNEXT -> mibMap.higherEntry(vb.oid).value
                    else -> mibMap.get(vb.oid)
                } ?: VariableBinding(vb.oid, noSuchObject).also {
                    errorStatus = PDU.noSuchName
                    if (errorIndex == 0) errorIndex = i
                }
            }
        }
        op(ev, resPdu0)?.let { resPdu ->
            val resTarget = CommunityTarget(ev.peerAddress, OctetString("public"))
            snmp.send(resPdu, resTarget)
        }
    }
}

@Suppress("unused")
val mibMapTest = sortedMapOf<OID, Variable>(
    OID(1, 3, 6, 1, 2, 1, 1, 1) to OctetString("AAAA"),
    OID(1, 3, 6, 1, 2, 1, 1, 2) to OID(1, 3, 6, 1, 2, 1, 1, 1, 1, 1),
    OID(1, 3, 6, 1, 2, 1, 1, 3) to TimeTicks(77777777),
    OID(1, 3, 6, 1, 2, 1, 1, 4) to Integer32(65535),
    OID(1, 3, 6, 9, 0) to OctetString("1.3.6.9.0"),
    OID(1, 3, 6, 9, 1, 2, 3) to OctetString("1.3.6.9.1.2.3")
).mapValues { (oid, v) -> VariableBinding(oid, v) }
    .entries.fold(TreeMap<OID, VariableBinding>()) { tree, e -> tree.apply { put(e.key, e.value) } }

fun OID(vararg ints: Int) = OID(ints)



