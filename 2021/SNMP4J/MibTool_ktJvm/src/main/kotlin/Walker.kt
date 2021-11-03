package sc

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.fluent.PduBuilder
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.*
import java.net.InetAddress
import java.util.concurrent.Semaphore

suspend fun main(args: Array<String>) {
    if (args.size == 0) {
        println("Syntax: java -jar walker.jar addr [topOid]")
        return
    }
    val addr = InetAddress.getByName(args[0])!!
    val topOid = OID(args.getOrNull(1) ?: "1")

    val snmpBuilder = SnmpBuilder()
    val snmp = snmpBuilder.udp().v1().threads(2).build()!!
    snmp.listen()

    val topPdu = PDU().apply { listOf(VariableBinding(topOid)) }
    val res = snmp.walk(addr, topPdu).toList().associate { it.oid!! to it.variable!! }.toSortedMap()
    println(res)
}

fun Snmp.walk(addr: InetAddress, topPdu: PDU): Flow<VariableBinding> {
    
}

fun SNMPManager.walkBulkAll(addr: String, initOid: List<Int> = listOf(1)): TreeMap<OID, VariableBinding> {
    val tm = TreeMap<OID, VariableBinding>()
    val semMaxSnmpRequest = Semaphore(1)
    semMaxSnmpRequest.acquire(1)
    walkBulk(addr, initOid, cbTerminate = { _, _ ->
        semMaxSnmpRequest.release()
    }) {
        tm[it.oid] = it
        SNMPManager.ResultCode.CONTINUE
    }
    semMaxSnmpRequest.acquire(1)
    return tm
}

fun VariableBinding.toVBFileString(): String {
    fun escape(it: Byte): String {
        return if (it < 0x20 || 0x7f < it || it == ':'.toByte() || it == '"'.toByte()) ":%02x".format(it.toInt() and 0xff)
        else it.toChar().toString()
    }

    val v = variable
    return oid.toString() + " " + variable.syntax + " " + when {
        v is OctetString -> "\"" + v.value.joinToString(separator = "") { escape(it) } + "\""
        v is OID -> v.toString()
        v is TimeTicks -> v.value.toString()
        // TBD...
        else -> v.toString()
    }
}


