import org.snmp4j.PDU
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.fluent.PduBuilder
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.Address
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import java.math.BigInteger
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.*


fun main(args: Array<String>) {
    val snmpBuilder = SnmpBuilder()
    val snmp = snmpBuilder.udp().v1().threads(2).build()
    snmp.listen()

    val netAdr = BigInteger(InetAddress.getByName(args.getOrNull(0) ?: "10.36.0.0").address).toLong()
    val scanMask = args.getOrNull(1)?.toInt() ?: 16
    for (i in 0L until (1 shl scanMask)) {
        val adrBytes = ByteBuffer.allocateDirect((netAdr or i.reverseBit32(scanMask)).toInt()).array()
        val udpAdr = UdpAddress(InetAddress.getByAddress(adrBytes), 161)
        val targetBuilder = snmpBuilder.target(udpAdr)
        val target = targetBuilder.community(OctetString("public"))
            .timeout(500).retries(10)
            .build()
        val pdu = targetBuilder.pdu().type(PDU.GETNEXT)
            .oids(* TargetOID.values().map { it.oid }.toTypedArray())
            .build()
        snmp.send(pdu, target, null, object : ResponseListener {
            override fun <A : Address?> onResponse(event: ResponseEvent<A>?) {
                TODO("Not yet implemented")
            }
        })
    }
    snmp.close()
}

fun Long.reverseBit32(width: Int = 32): Long {
    var x = this
    x = ((x and 0x55555555) shl 1) or ((x and 0xAAAAAAAA) ushr 1)
    x = ((x and 0x33333333) shl 2) or ((x and 0xCCCCCCCC) ushr 2)
    x = ((x and 0x0F0F0F0F) shl 4) or ((x and 0xF0F0F0F0) ushr 4)
    x = ((x and 0x00FF00FF) shl 8) or ((x and 0xFF00FF00) ushr 8)
    return ((x shl 16) or (x ushr 16)) ushr (32 - width)
}

enum class TargetOID(val oid: String) {
    sysDescr(sysDescr.oid),
    sysName(sysName.oid),
    hrDeviceDescr("1.3.6.1.2.1.25.3.2.1.3"),
    prtGeneralPrinterName("1.3.6.1.2.1.43.5.1.1.16"),
    prtInputVendorName("1.3.6.1.2.1.43.8.2.1.14"),
    prtOutputVendorName("1.3.6.1.2.1.43.9.2.1.8"),
}
