package  mibtool.snmp4jWrapper

import org.snmp4j.*
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.smi.*
import org.snmp4j.PDU
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.io.File
import java.util.*
import java.util.concurrent.Semaphore

typealias MibMap = TreeMap<OID, VariableBinding>

fun MibMap.getNext(oid: OID) = higherEntry(oid)?.value

fun main(args: Array<String>) {
    val mibMap = File(args[0]).run {
        val lines = readLines()
        val nMib = lines[0].toInt()
        lines.drop(1).asSequence().take(nMib).map { it.toVariableBinding() }
    }.run { MibMap().also { m -> forEach { m[it.oid!!] = it } } }
    mibMap.map { (a, b) -> b }.forEachIndexed { i, v ->
        println("$i $v")
    }
    val tm = DefaultUdpTransportMapping(UdpAddress("0.0.0.0".toInetAddr(), 161))
    Snmp(tm).use { snmp ->
        snmp.addCommandResponder(
                object : CommandResponder {
                    override fun <A : Address> processPdu(ev: CommandResponderEvent<A>) {
                        val resVBL = ev.pdu.variableBindings.map { vb ->
                            when (ev.pdu.type) {
                                PDU.GETNEXT -> mibMap.getNext(vb.oid)
                                else -> mibMap.get(vb.oid)
                            } ?: VariableBinding(vb.oid, Null.endOfMibView)
                        }
                        val resPdu = PDU(ev.pdu).apply {
                            type = PDU.RESPONSE
                            variableBindings = resVBL
                            errorStatus = if (resVBL.all { it.variable != Null.endOfMibView }) 0 else PDU.noSuchName
                            errorIndex = if (errorStatus == 0) 0 else resVBL.map { it.variable }.indexOf(Null.endOfMibView)
                        }
                        val target = CommunityTarget<A>().apply {
                            community = OctetString(ev.securityName)
                            address = ev.peerAddress
                            version = SnmpConstants.version1
                            timeout = 0
                            retries = 0
                        }
                        println("Req:${ev.peerAddress} PDU:{ty:${PDU.getTypeString(ev.pdu.type)}, vb:${ev.pdu.variableBindings} "
                                + "=> ResPDU:{ty:${PDU.getTypeString(resPdu.type)} er:${resPdu.errorStatus} ei:${resPdu.errorIndex} vb:${resPdu.variableBindings}"
                        )
                        snmp.send(resPdu, target)
                    }
                }
        )
        snmp.listen()
        println("started.")
        Semaphore(0).acquire()
    }
    println("term.")
}



fun Variable.toVariableString() = when (this) {
    is Integer32 -> value.toString() // 2: Integer32
    is OctetString -> value!!.caped() // 4: OctetString
    is Null -> ByteArray(0).caped() // 5: Null
    is OID -> toOidString() // 6: OID
    is IpAddress -> inetAddress.address!!.caped() // 64: IpAddress
    is Counter32 -> value.toString() // 65: Counter32
    is Gauge32 -> value.toString() // 66: Gauge32
    is TimeTicks -> value.toString() // 67: TimeTicks
    is Opaque -> value!!.caped() // 68: Opaque
    is Counter64 -> value.toString() // 70: Counter64
    //128 -> Null(128) // 128: NOSUCHOBJECT (Error)
    //129 -> Null(129) // 129: NOSUCHINSTANCE (Error)
    //130 -> Null(130) // 130: ENDOFMIBVIEW (Error)
    else -> throw IllegalArgumentException("Unsupported variable type: ${javaClass.name}")
}

fun OID.toOidString() = value.joinToString(".")
fun IntArray.toOidString() = joinToString(".", "\"", "\"")
fun String.uncaped2() = ("  " + this).windowed(3).mapNotNull {
    when {
        it[0] == ':' -> it.drop(1).toInt(16).toChar()
        it[1] == ':' -> null
        it[2] == ':' -> null
        else -> it[2]
    }
}.joinToString("")


fun ByteArray.caped() = toUByteArray().map { it.toInt() }.joinToString("", "\"", "\"") {
    if (it <= 0x20 || 0x7f <= it || it == '\"'.toInt() || it == ':'.toInt()) ":%02x".format(it)
    else it.toChar().toString()
}

fun String.uncaped() = generateSequence(0 to 0.toByte()) { (i, c) ->
    when {
        i >= length -> null
        this[i] == ':' -> (i + 3) to substring(i + 1, i + 3).toInt(16).toByte()
        else -> (i + 1) to this[i].toByte()
    }
}.drop(1).map { it.second }

/*
fun String.toVB(): VariableBinding {
    fun String.getToken(): Pair<String?, String> {
        val s1 = this.dropWhile { it == ' ' || it == '\t' }
        if (s1.length == 0) return null to ""
        if (s1[0] == '\"') {
            //s1.drop(1).spl
        }
        return "" to ""
    }

}
*/
