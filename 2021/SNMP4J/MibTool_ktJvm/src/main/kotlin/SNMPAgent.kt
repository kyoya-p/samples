import org.snmp4j.*
import org.snmp4j.mp.MPv1
import org.snmp4j.mp.MPv3
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.security.SecurityProtocols
import org.snmp4j.security.USM
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.io.File
import java.net.InetAddress
import java.util.*
import java.util.concurrent.Semaphore

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("usage: java -jar agent.jar oidFileName.txt")
        System.exit(-1)
    }
    val oidFile = args[0]
    val oidMap = Scanner(File(oidFile)).nextMIBSet()
    val ag = SNMPAgent(oidMap)
    ag.run()
    ag.idle() //ずっと待つ
}

// .oidファイルパーサ
// usage: Scanner(File(oidFile)).nextMIBSet()
fun Scanner.nextMIBSet(): TreeMap<OID, VariableBinding> {
    fun Scanner.nextVariable(): Variable {
        fun Scanner.nextOctetString(): ByteArray {
            next("\"(.*)\"")
            return Scanner(match().group(1)).run {
                generateSequence { true }.takeWhile { hasNext() }.map {
                    val b: Byte = findInLine(".").toCharArray()[0].toByte()
                    if (b == ':'.toByte()) findInLine("..").toInt(16).toByte() else b
                }.toList().toByteArray()
            }
        }

        return when (val valueType = nextInt()) {
            2 -> Integer32(nextInt())
            4 -> OctetString(nextOctetString())
            5 -> Null()
            6 -> OID(next())
            64 -> IpAddress(nextOctetString())
            65 -> Counter32(nextLong())
            66 -> Gauge32(nextLong())
            67 -> TimeTicks(nextLong())
            68 -> Opaque(nextOctetString())
            70 -> Counter64(nextLong())
            128 -> Null(128)
            129 -> Null(129)
            130 -> Null(130)
            else -> throw IllegalArgumentException("Unsupported variable syntax: ${valueType}")
        }
    }

    val size = nextInt()
    val m = TreeMap<OID, VariableBinding>()
    for (i in 0 until size) {
        val oid = OID(next())
        val variable = nextVariable()
        m[oid] = VariableBinding(oid, variable)
    }
    return m
}

class SNMPAgent(val mib: TreeMap<OID, VariableBinding>
                , val snmp: Snmp = Snmp(DefaultUdpTransportMapping(UdpAddress(InetAddress.getByName("0.0.0.0"), 161)))
) {

    inline fun makeResPdu_get(reqPdu: PDU): PDU {
        val resPdu = PDU().apply {
            type = PDU.RESPONSE
        }
        reqPdu.variableBindings.forEachIndexed { i, vb ->
            val resVb = mib[vb.oid]
            if (resVb == null) {
                resPdu.errorStatus = PDU.noSuchName
                resPdu.errorIndex = i + 1
                resPdu.add(VariableBinding(vb.oid, Null.endOfMibView))
            } else {
                reqPdu.add(resVb)
            }
        }
        return resPdu
    }

    inline fun makeResPdu_getNext(reqPdu: PDU): PDU {
        val resPdu = PDU().apply {
            type = PDU.RESPONSE
        }
        reqPdu.variableBindings.forEachIndexed { i, vb ->
            val resVb: VariableBinding? = mib.higherEntry(vb.oid)?.value
            if (resVb == null) {
                resPdu.errorStatus = PDU.noSuchName
                resPdu.errorIndex = i + 1
                resPdu.add(VariableBinding(vb.oid, Null.endOfMibView))
            } else {
                reqPdu.add(resVb)
            }
        }
        return resPdu
    }

    inline fun <T, R> Iterable<T>.mapWithHandover(initialValue: R, transform: (R) -> R): List<R> {
        val destination = ArrayList<R>()
        var passValue = initialValue
        for (item in this) {
            val ret = transform(passValue)
            destination.add(ret)
            passValue = ret
        }
        return destination
    }

    fun <T> generateLimitedSequence(repeateMax: Int, seed: T, nextFunction: (T) -> T?): Sequence<T> {
        return generateSequence(Pair(0, seed)) {
            if (it.first > repeateMax) null
            else {
                val nextVal = nextFunction(it.second)
                if (nextVal == null) null
                else Pair(it.first + 1, nextVal)
            }
        }.map { it.second }
    }

    // Ref:
    // https://www.alaxala.com/jp/techinfo/archive/manual/AX3640S/HTML/11_12/CFGUIDE2/0413.HTM
    fun makeResPdu_getBulk(reqPdu: PDU): PDU {
        val resPdu = PDU().apply { type = PDU.RESPONSE }
        val bulkRepeate = reqPdu.errorIndex
        generateLimitedSequence(bulkRepeate, reqPdu.variableBindings) {
            it.mapIndexed { i, vb ->
                val resVB = mib.higherEntry(vb.oid)?.value
                if (resVB == null) {
                    resPdu.errorStatus = PDU.noSuchName
                    resPdu.errorIndex = i + 1
                    VariableBinding(vb.oid, Null.endOfMibView)
                } else {
                    resVB
                }
            }.mapTo(Vector<VariableBinding>()) { it }
        }.flatMap { it.asSequence() }.forEach { resPdu.add(it) }
        return resPdu
    }

    fun makeResPdu_getBulkXX(reqPdu: PDU): PDU {
        val resPdu = PDU().apply {
            type = PDU.RESPONSE
        }
        val bulkRepeate = reqPdu.errorIndex
        val v = Vector<VariableBinding>()
        (0 until bulkRepeate).mapWithHandover(reqPdu.variableBindings) {
            val variableBindingsElement = Vector<VariableBinding>()
            it.mapIndexed { i, vb ->
                val resVB = mib.higherEntry(vb.oid)?.value
                if (resVB == null) {

                    resPdu.errorStatus = PDU.noSuchName
                    resPdu.errorIndex = i + 1
                    val resVB = VariableBinding(vb.oid, Null.endOfMibView)
                    variableBindingsElement.add(resVB)
                    resPdu.add(resVB)
                    resVB
                } else {
                    variableBindingsElement.add(resVB)
                    resPdu.add(resVB)
                    resVB
                }
            }.forEach { v.add(it) }
            v
        }
        return resPdu
    }

    fun run(): SNMPAgent {
        snmp.listen()
        snmp.addCommandResponder { ev ->
            print(ev.pdu)
            val resPdu = PDU(ev.pdu).apply {
                type = PDU.RESPONSE
                variableBindings.clear()
            }

            ev.pdu.variableBindings.mapIndexed { i, vb ->
                val resVB = if (ev.pdu.type == PDU.GETNEXT) mib.higherEntry(vb.oid)?.value else mib[vb.oid]
                if (resVB != null) resVB
                else {
                    resPdu.errorStatus = PDU.noSuchName
                    resPdu.errorIndex = i + 1
                    VariableBinding(vb.oid, vb.variable)
                }
            }.forEach {
                resPdu.add(it)
            }

            val target = CommunityTarget().apply {
                community = OctetString("public")
                address = ev.peerAddress
                version = SnmpConstants.version1
                timeout = 5000
                retries = 5
            }

            snmp.send(resPdu, target)
            println(" -> $resPdu")
        }

        println("listening.")
        return this
    }

    // Test
    fun run2(): SNMPAgent {
        snmp.listen()
        snmp.addCommandResponder { ev ->
            print(ev.pdu)
            val resPdu = PDU(ev.pdu).apply {
                type = PDU.RESPONSE
                variableBindings.clear()
            }

            val bulkRepeate = if (ev.pdu.type == PDU.GETBULK) ev.pdu.errorIndex else 1
            (0 until bulkRepeate).mapWithHandover(ev.pdu.variableBindings) {
                val variableBindingsElement = Vector<VariableBinding>()
                it.mapIndexed { i, vb ->
                    val resVB = if ((ev.pdu.type == PDU.GETNEXT) or (ev.pdu.type == PDU.GETBULK)) mib.higherEntry(vb.oid)?.value else mib[vb.oid]
                    if (resVB != null) resVB
                    else if ((resVB == null) and (ev.pdu.type == PDU.GETBULK)) VariableBinding(vb.oid, Null.endOfMibView)
                    else {
                        resPdu.errorStatus = PDU.noSuchName
                        resPdu.errorIndex = i + 1
                        VariableBinding(vb.oid, vb.variable)
                    }
                }.forEach {
                    variableBindingsElement.add(it)
                }
                variableBindingsElement
            }
            val target = CommunityTarget().apply {
                community = OctetString("public")
                address = ev.peerAddress
                version = SnmpConstants.version1
                timeout = 5000
                retries = 5
            }

            snmp.send(resPdu, target)
            println(" -> $resPdu")
        }

        println("listening.")
        return this
    }

    // Test
    fun idle(): SNMPAgent {
        Semaphore(0).acquire()
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
        val tm = DefaultUdpTransportMapping(UdpAddress(InetAddress.getByName("0.0.0.0"), 2001))
        md.addTransportMapping(tm)
        val snmp = Snmp(tm)

        snmp.addCommandResponder({ ev ->
            println(ev.pdu)
        })
        snmp.listen()
    }

}

