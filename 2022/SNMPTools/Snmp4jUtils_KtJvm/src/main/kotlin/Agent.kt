package jp.wjg.shokkaa.snmp4jutils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.*
import org.snmp4j.*
import org.snmp4j.smi.*
import org.snmp4j.smi.Null.noSuchObject
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.io.File
import java.net.InetAddress
import java.util.*

@ExperimentalSerializationApi
fun main(args: Array<String>) {
    val dev: Device = yamlSnmp4j.decodeFromStream(File(args[0]).inputStream())
    SnmpAgentX(dev.vbl.associate { it.oid to it.variable })
}

@Suppress("BlockingMethodInNonBlockingContext", "BlockingMethodInNonBlockingContext")
suspend fun snmpAgent(
    vbl: List<VariableBinding>,
    snmp: Snmp = Snmp(
        DefaultUdpTransportMapping(
            UdpAddress(
                InetAddress.getByName("0.0.0.0"),
                161
            )
        )
    ).apply { listen() },
    hook: (ResponderEvent, PDU) -> PDU = { _, pdu -> pdu },
) {
    val oidVBMap = TreeMap<OID, VariableBinding>().apply {
        vbl.forEach { put(it.oid, VariableBinding(it.oid, it.variable)) }
    }
    snmpAgent(snmp) { event ->
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
        snmp.send(hook(event, resPdu), resTarget)
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun snmpAgent(
    snmp: Snmp,
    responseHandler: suspend (ResponderEvent) -> Unit,
) {
    fun commandResponder(responseHandler: (ResponderEvent) -> Unit) = object : CommandResponder {
        override fun <A : Address?> processPdu(event: CommandResponderEvent<A>?) {
            @Suppress("UNCHECKED_CAST")
            responseHandler(event as CommandResponderEvent<UdpAddress>)
        }
    }
    println("Start agent")
    callbackFlow {
        snmp.addCommandResponder(commandResponder { trySend(it) })
        awaitClose { snmp.close() }
    }.collect { responseHandler(it) }
}

class SnmpAgentX(
    val mibMap: Map<OID, Variable>,
    val snmp: Snmp = Snmp(
        DefaultUdpTransportMapping(
            UdpAddress(
                InetAddress.getByName("0.0.0.0"),
                161
            )
        )
    ).apply { listen() },
) {
    val oidVBMap = TreeMap<OID, VariableBinding>().apply {
        mibMap.forEach { oid, v -> put(oid, VariableBinding(oid, v)) }
    }

    fun commandResponder(
        responseHandler: (ResponderEvent, PDU?) -> PDU? = { _, pdu -> pdu },
    ) = object : CommandResponder {
        override fun <A : Address?> processPdu(event: CommandResponderEvent<A>?) {
            if (event == null) return
            val resPdu: PDU? = PDU().run {
                type = PDU.RESPONSE
                requestID = event.pdu.requestID
                errorIndex = 0
                errorStatus = PDU.noError
                variableBindings = event.pdu.variableBindings.mapIndexed { i, vb ->
                    when (event.pdu.type) {
                        PDU.GETNEXT -> oidVBMap.higherEntry(vb.oid).value
                        else -> oidVBMap.get(vb.oid)
                    } ?: VariableBinding(vb.oid, noSuchObject).also {
                        errorStatus = PDU.noSuchName
                        if (errorIndex == 0) errorIndex = i
                    }
                }
                @Suppress("UNCHECKED_CAST")
                responseHandler(event as ResponderEvent, this)
            }
            if (resPdu == null) return

            val resTarget = CommunityTarget(event.peerAddress, OctetString("public"))
            snmp.send(resPdu, resTarget)
        }
    }

    fun start(resHandler: ResponseHandler = { _, pdu -> pdu }) =
        commandResponder(resHandler).also { snmp.addCommandResponder(it) }

    fun stop(commandRdesponder: CommandResponder) = snmp.removeCommandResponder(commandRdesponder)
    fun close() = snmp.close()

    fun <R> use(block: (SnmpAgentX) -> R): R {
        val handler = start()
        val r = block(this)
        stop(handler)
        close()
        return r
    }

    @ExperimentalCoroutinesApi
    suspend fun serviceFlow(onClose: () -> Unit = {}, onStart: () -> Unit = {}) = callbackFlow {
        val responderHandler = start { event, pdu ->
            trySend(event)
            pdu
        }
        println("onStart()")
        onStart()
        awaitClose {
            println("awaitClose()")
            stop(responderHandler)
            snmp.close()
            onClose()
        }
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



