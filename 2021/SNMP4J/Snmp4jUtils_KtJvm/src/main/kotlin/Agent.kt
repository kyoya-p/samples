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
    SnmpAgent(dev.vbl.associate { it.oid to it.variable })
}

suspend fun snmpAgent(
    snmp: Snmp = Snmp(DefaultUdpTransportMapping(UdpAddress(InetAddress.getByName("0.0.0.0"),
        161))).apply { listen() },
    mibMap: Map<OID, Variable>,
) {
    val oidVBMap = TreeMap<OID, VariableBinding>().apply {
        mibMap.forEach { oid, v -> put(oid, VariableBinding(oid, v)) }
    }
    snmpAgent(snmp) { event ->
        val resPdu = PDU().apply {
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
        }

        val resTarget = CommunityTarget(event.peerAddress, OctetString("public"))
        snmp.send(resPdu, resTarget)
    }
}

suspend fun snmpAgent(
    snmp: Snmp = Snmp(DefaultUdpTransportMapping(UdpAddress(InetAddress.getByName("0.0.0.0"),
        161))).apply { listen() },
    responseHandler: suspend (ResponderEvent) -> Unit,
) {
    fun commandResponder(responseHandler: (ResponderEvent) -> Unit) = object : CommandResponder {
        override fun <A : Address?> processPdu(event: CommandResponderEvent<A>?) {
            @Suppress("UNCHECKED_CAST")
            responseHandler(event as CommandResponderEvent<UdpAddress>)
        }
    }
    callbackFlow {
        snmp.addCommandResponder(commandResponder { trySend(it) })
        awaitClose { }
    }.collect {
        responseHandler(it)
    }
}

class SnmpAgent(
    val mibMap: Map<OID, Variable>,
    val snmp: Snmp = Snmp(DefaultUdpTransportMapping(UdpAddress(InetAddress.getByName("0.0.0.0"),
        161))).apply { listen() },
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

    fun <R> use(block: (SnmpAgent) -> R): R {
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
val mibMapTest = sortedMapOf<OID, Variable>(
    OID(1, 3, 6, 1, 2, 1, 1, 1) to OctetString("AAAA"),
    OID(1, 3, 6, 1, 2, 1, 1, 2) to OID(1, 3, 6, 1, 2, 1, 1, 1, 1, 1),
    OID(1, 3, 6, 1, 2, 1, 1, 3) to TimeTicks(77777777),
    OID(1, 3, 6, 1, 2, 1, 1, 4) to Integer32(65535),
    OID(1, 3, 6, 9, 0) to OctetString("1.3.6.9.0"),
    OID(1, 3, 6, 9, 1, 2, 3) to OctetString("1.3.6.9.1.2.3")
).mapValues { (oid, v) -> VariableBinding(oid, v) }
    .entries.fold(TreeMap<OID, VariableBinding>()) { tree, e -> tree.apply { put(e.key, e.value) } }



