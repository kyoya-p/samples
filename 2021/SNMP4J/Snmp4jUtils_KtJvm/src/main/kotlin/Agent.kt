package jp.pgw.shokkaa

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.snmp4j.*
import org.snmp4j.smi.*
import org.snmp4j.smi.Null.noSuchObject
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.net.InetAddress
import java.util.*

typealias ResponderEvent = CommandResponderEvent<UdpAddress>

@Suppress("BlockingMethodInNonBlockingContext")
@ExperimentalCoroutinesApi
suspend fun snmpAgentFlow(
    mibMap: TreeMap<OID, VariableBinding>,
    snmp: Snmp = Snmp(DefaultUdpTransportMapping(UdpAddress(InetAddress.getByName("0.0.0.0"), 161))).apply { listen() },
    op: (ev: ResponderEvent, resPdu: PDU) -> PDU? = { _, pdu -> pdu },
) = callbackFlow {
    val commandResponder = object : CommandResponder {
        override fun <A : Address?> processPdu(event: CommandResponderEvent<A>?) {
            println("processPdu()")
            if (event == null) return
            val resPdu = PDU().run {
                type = PDU.RESPONSE
                requestID = event.pdu.requestID
                errorIndex = 0
                errorStatus = PDU.noError
                variableBindings = event.pdu.variableBindings.mapIndexed { i, vb ->
                    when (event.pdu.type) {
                        PDU.GETNEXT -> mibMap.higherEntry(vb.oid).value
                        else -> mibMap.get(vb.oid)
                    } ?: VariableBinding(vb.oid, noSuchObject).also {
                        errorStatus = PDU.noSuchName
                        if (errorIndex == 0) errorIndex = i
                    }
                }
                @Suppress("UNCHECKED_CAST")
                op(event as ResponderEvent, this)
            }

            val resTarget = CommunityTarget(event.peerAddress, OctetString("public"))
            snmp.send(resPdu, resTarget)
            println("snmp.send()")

            @Suppress("UNCHECKED_CAST")
            trySend(event as ResponderEvent)
        }
    }

    runCatching {
        snmp.addCommandResponder(commandResponder)
        println("started Agent")
        awaitClose {
            snmp.removeCommandResponder(commandResponder)
            println("canceled Agent")
        }
    }.onFailure { close() }
    println("finished Agent")
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



