package jp.`live-on`.shokkaa

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.snmp4j.*
import org.snmp4j.mp.MPv1
import org.snmp4j.mp.MPv3
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.security.SecurityProtocols
import org.snmp4j.security.USM
import org.snmp4j.smi.*
import org.snmp4j.smi.Null.noSuchObject
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.io.File
import java.net.Inet4Address
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
    runCatching {
        snmp.addCommandResponder(commandResponder)
        awaitClose()
        snmp.removeCommandResponder(commandResponder)
    }.onFailure {
        snmp.removeCommandResponder(commandResponder)
    }
}

@Suppress("BlockingMethodInNonBlockingContext", "unused")
@ExperimentalCoroutinesApi
suspend fun snmpAgent(
    mibMap: TreeMap<OID, VariableBinding>,
    host: String = "0.0.0.0",
    port: Int = 161,
) = snmpAgentFlow(Snmp(DefaultUdpTransportMapping(UdpAddress(InetAddress.getByName(host), port)))).collectLatest { ev ->
    val resPdu = ev.pdu
    resPdu.apply {
        type = PDU.RESPONSE
        errorIndex = 0
        errorStatus = PDU.noError
        variableBindings = ev.pdu.variableBindings.mapIndexed { i, vb ->
            when (ev.pdu.type) {
                PDU.GETNEXT -> mibMap.get(vb.oid)
                else -> mibMap.higherEntry(vb.oid).value
            } ?: VariableBinding(vb.oid, noSuchObject).also {
                errorStatus = PDU.noSuchName
                if (errorIndex == 0) errorIndex = i
            }
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


// ----------------------------------------------

fun interface MIBMapper {
    fun requestEvent(ev: CommandResponderEvent<UdpAddress>): PDU?
}


fun OID(vararg ints: Int) = OID(ints)


@Suppress("unused")
class SNMPAgent(
    val snmp: Snmp = Snmp(DefaultUdpTransportMapping(UdpAddress(InetAddress.getByName("0.0.0.0"), 161))),
    val mibMapper: MIBMapper,
) {
    companion object {
        @ExperimentalSerializationApi
        fun from(snmp: Snmp, mibFile: File): SNMPAgent {
            val vbl = TreeMap<OID, VariableBinding>()
            jsonSnmp4j.decodeFromStream<List<VariableBinding>>(mibFile.inputStream()).forEach {
                vbl[it.oid] = it
            }
            return from(snmp, vbl)
        }

        fun from(snmp: Snmp, mibMap: TreeMap<OID, VariableBinding>): SNMPAgent {
            return SNMPAgent(snmp) { ev ->
                val resPdu = ev.pdu
                resPdu.apply {
                    type = PDU.RESPONSE
                    errorIndex = 0
                    errorStatus = PDU.noError
                    variableBindings = ev.pdu.variableBindings.mapIndexed { i, vb ->
                        when (ev.pdu.type) {
                            PDU.GETNEXT -> mibMap.get(vb.oid)
                            else -> mibMap.higherEntry(vb.oid).value
                        } ?: VariableBinding(vb.oid, noSuchObject).also {
                            errorStatus = PDU.noSuchName
                            if (errorIndex == 0) errorIndex = i
                        }
                    }
                }
            }
        }
    }

    fun start(
        addr: InetAddress = Inet4Address.getByName("0.0.0.0"),
        port: Int = 161,
        cbResponse: (PDU, PDU) -> Unit,
    ): SNMPAgent {
        val ip = addr
        println("SNMPAgent.start() [ip=${ip}, port=$port]")
        return start(cbResponse)
    }

    fun start(cbResponse: (PDU, PDU) -> Unit): SNMPAgent {
        snmp.addCommandResponder { ev: CommandResponderEvent<UdpAddress> ->
            val resPdu = mibMapper.requestEvent(ev)!!

            val target = CommunityTarget<UdpAddress>().apply {
                community = OctetString("public")
                address = ev.peerAddress
                version = SnmpConstants.version1
                timeout = 0
                retries = 0
            }
            cbResponse(ev.pdu, resPdu)
            snmp.send(resPdu, target)
        }
        snmp.listen()
        return this
    }

    // TBD
    fun snmpV3() {

        // SNMPV3
        val md: MessageDispatcher = MessageDispatcherImpl()

        val usm = USM(SecurityProtocols.getInstance(),
            OctetString(MPv3.createLocalEngineID()), 0)
        md.addMessageProcessingModel(MPv1())
        md.addMessageProcessingModel(MPv3(usm))

        val tm = DefaultUdpTransportMapping(UdpAddress(Inet4Address.getByName("0.0.0.0"), 2001))
        md.addTransportMapping(tm)
        val snmp = Snmp(tm)


        //  SecurityModels.getInstance().addSecurityModel(usm)
        /* snmp.usm.addUser(OctetString("MD5DES"),
            UsmUser(OctetString("MD5DES"),
                    AuthMD5.ID,
                    OctetString("MD5DESUserAuthPassword"),
                    PrivDES.ID,
                    OctetString("MD5DESUserPrivPassword")))
    */
        snmp.addCommandResponder({ ev ->
            println(ev.pdu)
        })
        snmp.listen()
    }

}

// 最近SAM変換うまくいかないな
fun Snmp.addCommandResponder(op: (CommandResponderEvent<UdpAddress>) -> Unit) =
    addCommandResponder(object : CommandResponder {
        override fun <A : Address?> processPdu(event: CommandResponderEvent<A>?) {
            @Suppress("UNCHECKED_CAST")
            op(event as CommandResponderEvent<UdpAddress>)
        }
    })